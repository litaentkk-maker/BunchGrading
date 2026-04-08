import os, sys, time, base64, signal, warnings, json, platform

from types import ModuleType
import importlib.util, importlib.machinery

# --- SIDEBAND/COLUMBA COMPATIBILITY MOCKS ---
# Suppress noisy library warnings
warnings.filterwarnings("ignore", category=DeprecationWarning)
warnings.filterwarnings("ignore", category=ResourceWarning)

# Mock signal.signal because Reticulum tries to register handlers on a background thread
_orig_signal = signal.signal
def _mock_signal(sig, handler):
    try:
        return _orig_signal(sig, handler)
    except ValueError:
        # Ignore "signal only works in main thread" error
        return None
signal.signal = _mock_signal

class Dummy:
    def __init__(self, name="Dummy"):
        self.__name__ = name
        self.__spec__ = importlib.machinery.ModuleSpec(name, None)
    def __getattr__(self, name): return self
    def __call__(self, *args, **kwargs): return self
    def __len__(self): return 0
    def __getitem__(self, index): return self

def mock_module(name):
    mock = Dummy(name)
    sys.modules[name] = mock
    return mock

mock_module("usbserial4a").serial4a = Dummy("serial4a")
mock_module("jnius").autoclass = lambda x: Dummy("DummyClass")
mock_module("usb4a").usb = Dummy("usb4a.usb")
sys.modules["usb4a.usb"] = sys.modules["usb4a"].usb

_orig_find_spec = importlib.util.find_spec
def _mock_find_spec(name, package=None):
    if name in ["usbserial4a", "jnius", "usb4a", "usb4a.usb"]: return sys.modules[name].__spec__
    return _orig_find_spec(name, package)
importlib.util.find_spec = _mock_find_spec

import RNS, LXMF
from LXMF import LXMRouter, LXMessage
from RNS.Interfaces.RNodeInterface import RNodeInterface
from RNS.Interfaces.TCPInterface import TCPClientInterface
from RNS.Interfaces.Interface import Interface

active_ifac = None
router = None; local_destination = None; kotlin_callback = None; is_rns_running = False

def log(msg):
    print(f"RNS-LOG: {msg}")
    sys.stdout.flush()
    if kotlin_callback:
        try:
            kotlin_callback.onStatusUpdate(msg)
        except Exception:
            pass

# We also need to patch RNS internal platform detection if it exists
try:
    import RNS
    if hasattr(RNS, "vendor") and hasattr(RNS.vendor, "platform"):
        RNS.vendor.platform.system = lambda: "Linux"
    if hasattr(RNS, "Platform"):
        RNS.Platform.is_android = lambda: False
except:
    pass

def start_rns(storage_path, callback_obj, nickname):
    global router, local_destination, kotlin_callback, is_rns_running
    kotlin_callback = callback_obj
    log(f"start_rns() called with storage_path: {storage_path}")
    log(f"DIAGNOSTIC: platform.system()={platform.system()}, sys.platform={sys.platform}")
    
    if is_rns_running and local_destination is not None: 
        log("RNS already running, returning existing hash")
        return RNS.hexrep(local_destination.hash, False)
    
    try:
        storage_path = str(storage_path)
        log("Setting up directories...")
        os.environ["TMPDIR"] = os.path.join(storage_path, "cache")
        rns_dir = os.path.join(storage_path, ".reticulum")
        lxmf_dir = os.path.join(storage_path, ".lxmf")
        
        # Ensure all directories exist with proper permissions
        for d in [os.environ["TMPDIR"], rns_dir, lxmf_dir]:
            if not os.path.exists(d):
                log(f"Creating directory: {d}")
                os.makedirs(d, mode=0o755)
        
        # Create or overwrite config
        config_path = os.path.join(rns_dir, "config")
        log(f"Writing Reticulum config at: {config_path}")
        with open(config_path, "w") as f:
            f.write("""[reticulum]
enable_transport = True
share_instance = No

[interfaces]
  # Explicitly disable automatic interface discovery and add a dummy/disabled UDPInterface
  # to prevent port binding conflicts common on Android.
  [[DummyUDP]]
    type = UDPInterface
    interface_enabled = False
    listen_ip = 127.0.0.1
    listen_port = 4242
    forward_ip = 127.0.0.1
    forward_port = 4242
""")
        
        # Initialize Reticulum
        log("Initializing Reticulum stack (this may take a moment)...")
        RNS.Reticulum(configdir=rns_dir)
        log("Reticulum stack initialized")
        
        # Load or create identity
        id_path = os.path.join(storage_path, "storage_identity")
        log(f"Loading identity from: {id_path}")
        local_id = RNS.Identity.from_file(id_path) if os.path.exists(id_path) else RNS.Identity()
        if not os.path.exists(id_path):
            log("Generating new identity...")
            local_id.to_file(id_path)
        
        # Setup router
        log("Setting up LXMF router...")
        router = LXMRouter(identity=local_id, storagepath=lxmf_dir)
        local_destination = router.register_delivery_identity(local_id, display_name=nickname)
        router.register_delivery_callback(on_lxmf)
        RNS.Transport.register_announce_handler(discovery_handler())
        is_rns_running = True
        
        log(f"RNS STARTED: {RNS.hexrep(local_destination.hash, False)}")
        return RNS.hexrep(local_destination.hash, False)
    except Exception as e:
        log(f"FATAL ERROR starting RNS: {e}")
        import traceback
        log(traceback.format_exc())
        is_rns_running = False
        return ""

class discovery_handler:
    def __init__(self): self.aspect_filter = None
    def received_announce(self, destination_hash, announced_identity, app_data):
        h = RNS.hexrep(destination_hash, False); n = app_data.decode("utf-8") if app_data else ""
        log(f"DISCOVERY: {h} ({n})")
        if kotlin_callback: kotlin_callback.onAnnounceReceived(h, n)

def on_lxmf(lxm):
    sender = RNS.hexrep(lxm.source_hash, False)
    log(f"LXMF RECEIVED from {sender}")
    content = lxm.content.decode("utf-8")
    if content.startswith("ACK:"):
        if kotlin_callback: kotlin_callback.onMessageDelivered(content[4:])
        return
    is_img = content.startswith("IMG:")
    if is_img:
        path = os.path.join(os.environ["TMPDIR"], f"rec_{int(time.time())}.webp")
        with open(path, "wb") as f: f.write(base64.b64decode(content[4:]))
        content = path
    if kotlin_callback: kotlin_callback.onNewMessage(sender, content, int(time.time()*1000), is_img, False, RNS.hexrep(lxm.hash, False))

def inject_rnode_json(params_json):
    try:
        params = json.loads(params_json)
        freq = params.get("freq", 433000000)
        bw = params.get("bw", 125000)
        tx = params.get("tx", 17)
        sf = params.get("sf", 8)
        cr = params.get("cr", 6)
        return inject_rnode(freq, bw, tx, sf, cr)
    except Exception as e:
        log(f"JSON Injection Error: {e}")
        return str(e)

def inject_rnode(freq, bw, tx, sf, cr):
    global active_ifac
    log(f"inject_rnode() CALLED - F:{freq} BW:{bw} SF:{sf} CR:{cr}")
    
    waited = 0
    while not is_rns_running and waited < 45:
        log(f"Waiting for RNS to start... ({waited}s)")
        time.sleep(1)
        waited += 1
    
    if not is_rns_running:
        log("FATAL: RNS never started, cannot inject interface")
        return "RNS_NOT_READY"

    try:
        if active_ifac is not None:
            try:
                log("Removing old interface...")
                if active_ifac in RNS.Transport.interfaces:
                    RNS.Transport.interfaces.remove(active_ifac)
                active_ifac.detach()
            except Exception as e:
                log(f"Error removing old interface: {e}")
            active_ifac = None

        ictx = {
            "name": "RNodeBridge", 
            "type": "RNodeInterface", 
            "interface_enabled": True, 
            "port": "socket://127.0.0.1:7633",
            "frequency": freq,
            "bandwidth": bw,
            "txpower": tx,
            "spreadingfactor": sf,
            "codingrate": cr,
            "flow_control": False
        }
        log(f"Injecting RNode interface via socket://127.0.0.1:7633")
        
        retries = 5
        while retries > 0:
            try:
                # --- PLATFORM MONKEYPATCH ---
                _has_android_arg = "ANDROID_ARGUMENT" in os.environ
                _old_android_arg = os.environ.get("ANDROID_ARGUMENT")
                if _has_android_arg:
                    del os.environ["ANDROID_ARGUMENT"]
                    
                _has_android_root = "ANDROID_ROOT" in os.environ
                _old_android_root = os.environ.get("ANDROID_ROOT")
                if _has_android_root:
                    del os.environ["ANDROID_ROOT"]

                # --- SERIAL MONKEYPATCH ---
                import serial
                if not hasattr(serial, "_rns_patched"):
                    _orig_serial = serial.Serial
                    def _patched_serial(*args, **kwargs):
                        port = kwargs.get('port') or (args[0] if args else None)
                        if port and str(port).startswith("socket://"):
                            if 'port' in kwargs:
                                del kwargs['port']
                            new_args = args[1:] if args else ()
                            return serial.serial_for_url(port, *new_args, **kwargs)
                        return _orig_serial(*args, **kwargs)
                    serial.Serial = _patched_serial
                    serial._rns_patched = True

                try:
                    active_ifac = RNodeInterface(RNS.Transport, ictx)
                    # Force interface to be active and ready for transmission
                    active_ifac.IN = True
                    active_ifac.OUT = True
                    active_ifac.mode = Interface.MODE_FULL
                    
                    if active_ifac not in RNS.Transport.interfaces:
                        RNS.Transport.interfaces.append(active_ifac)
                finally:
                    if _has_android_arg:
                        os.environ["ANDROID_ARGUMENT"] = _old_android_arg
                    if _has_android_root:
                        os.environ["ANDROID_ROOT"] = _old_android_root
                
                log(f"Interface Injection Done. Status: {active_ifac}")
                
                # Allow some time for KISS initialization over the bridge
                time.sleep(2.0)
                
                # Start periodic announce loop in a separate thread
                def _announce_loop():
                    while is_rns_running:
                        try:
                            time.sleep(900) # Every 15 minutes
                            announce_now()
                        except:
                            pass
                threading.Thread(target=_announce_loop, daemon=True).start()
                break
            except Exception as e:
                retries -= 1
                log(f"Interface connection failed, retrying... ({retries} left): {e}")
                time.sleep(1.0)
        
        if retries == 0:
            log("FATAL: Failed to connect to local TCP bridge after 5 attempts")
            return "OFFLINE"
            
        return "ONLINE"
    except Exception as e: 
        log(f"Injection Error: {str(e)}")
        import traceback
        log(traceback.format_exc())
        return str(e)

def heartbeat():
    """Simple heartbeat to verify Python is still responsive"""
    return int(time.time())

def send_text(dest_hex, text):
    try:
        log(f"LXMF SENDING to {dest_hex}...")
        dest_hash = bytes.fromhex(dest_hex)
        dest_id = RNS.Identity.recall(dest_hash)
        
        if dest_id:
            dest = RNS.Destination(dest_id, RNS.Destination.OUT, RNS.Destination.SINGLE, "lxmf", "delivery")
        else:
            # Create destination with hash if identity is unknown
            dest = RNS.Destination(None, RNS.Destination.OUT, RNS.Destination.SINGLE, "lxmf", "delivery")
            dest.hash = dest_hash
            log(f"Identity for {dest_hex} unknown, sending as hash-only destination")
        
        lxm = LXMessage(dest, local_destination, text)
        router.handle_outbound(lxm)
        
        # Force transport to process
        try:
            RNS.Transport.process_outbound_queue()
        except:
            pass
            
        return RNS.hexrep(lxm.hash, False)
    except Exception as e:
        log(f"Send Error: {e}")
        import traceback
        log(traceback.format_exc())
        return ""

def send_report(target_hex, harvester_nick, block_id, ripe, empty, lat, lon, ts_str, photo_b64):
    try:
        # ALIGNED CSV SCHEMA: id, harvester_id, block_id, ripe_bunches, empty_bunches, latitude, longitude, timestamp, photo_file
        report_id = f"R{int(time.time())}"
        csv_payload = f"id,harvester_id,block_id,ripe_bunches,empty_bunches,latitude,longitude,timestamp,photo_file\n"
        csv_payload += f"{report_id},{harvester_nick},{block_id},{ripe},{empty},{lat},{lon},{ts_str},{photo_b64}"
        
        return send_text(target_hex, csv_payload)
    except Exception as e:
        log(f"Report Error: {e}")
        return ""

def send_image(dest_hex, path):
    with open(path, "rb") as f: data = base64.b64encode(f.read()).decode("utf-8")
    return send_text(dest_hex, f"IMG:{data}")

def announce_now():
    if local_destination:
        log("SENDING ANNOUNCE...")
        local_destination.announce(app_data=local_destination.display_name.encode("utf-8"))
        # Force transport to process outbound queue
        try:
            RNS.Transport.process_outbound_queue()
        except:
            pass
