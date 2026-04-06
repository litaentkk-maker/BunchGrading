import os, sys, time, base64, signal, warnings, json, platform, pty, threading
import socket as sock_mod

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
_pty_thread = None
_pty_master_fd = None

def _pty_tcp_bridge(master_fd, tcp_host, tcp_port):
    """Bridges a pty master fd to a TCP socket. Runs in a background thread."""
    log(f"PTY-TCP bridge thread starting, connecting to {tcp_host}:{tcp_port}")
    try:
        s = sock_mod.socket(sock_mod.AF_INET, sock_mod.SOCK_STREAM)
        s.connect((tcp_host, tcp_port))
        log("PTY-TCP bridge: TCP connected")

        def pty_to_tcp():
            try:
                while True:
                    data = os.read(master_fd, 2048)
                    if not data:
                        break
                    s.sendall(data)
            except Exception as e:
                log(f"PTY->TCP error: {e}")

        def tcp_to_pty():
            try:
                while True:
                    data = s.recv(2048)
                    if not data:
                        break
                    os.write(master_fd, data)
            except Exception as e:
                log(f"TCP->PTY error: {e}")

        t1 = threading.Thread(target=pty_to_tcp, daemon=True)
        t2 = threading.Thread(target=tcp_to_pty, daemon=True)
        t1.start(); t2.start()
        t1.join(); t2.join()
    except Exception as e:
        log(f"PTY-TCP bridge failed: {e}")
    finally:
        try: s.close()
        except: pass
        log("PTY-TCP bridge thread exited")

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
    global active_ifac, _pty_thread, _pty_master_fd

    log(f"inject_rnode() CALLED - F:{freq} BW:{bw} SF:{sf} CR:{cr}")

    waited = 0
    while not is_rns_running and waited < 45:
        log(f"Waiting for RNS... ({waited}s)")
        time.sleep(1)
        waited += 1
    if not is_rns_running:
        log("FATAL: RNS never started")
        return "RNS_NOT_READY"

    try:
        # Tear down old interface
        if active_ifac is not None:
            try:
                if active_ifac in RNS.Transport.interfaces:
                    RNS.Transport.interfaces.remove(active_ifac)
                active_ifac.detach()
            except Exception as e:
                log(f"Error removing old interface: {e}")
            active_ifac = None

        # Close old pty if any
        if _pty_master_fd is not None:
            try: os.close(_pty_master_fd)
            except: pass
            _pty_master_fd = None

        # Create pty pair: master_fd <-> slave_fd
        master_fd, slave_fd = pty.openpty()
        slave_path = os.ttyname(slave_fd)
        log(f"PTY created: slave={slave_path}")
        _pty_master_fd = master_fd

        # Start bridge thread: master_fd <-> TCP 127.0.0.1:7633
        _pty_thread = threading.Thread(
            target=_pty_tcp_bridge,
            args=(master_fd, "127.0.0.1", 7633),
            daemon=True
        )
        _pty_thread.start()
        time.sleep(0.3)  # Let bridge thread connect

        # Now give the slave pty path to RNodeInterface as if it's a real serial port
        # Temporarily hide Android env vars so RNodeInterface doesn't bail out
        for var in ["ANDROID_ARGUMENT", "ANDROID_ROOT", "ANDROID_DATA"]:
            os.environ.pop(var, None)

        ictx = {
            "name": "RNodeBridge",
            "type": "RNodeInterface",
            "interface_enabled": True,
            "port": slave_path,          # e.g. /dev/pts/3
            "frequency": int(freq),
            "bandwidth": int(bw),
            "txpower": int(tx),
            "spreadingfactor": int(sf),
            "codingrate": int(cr),
            "flow_control": False,
        }
        log(f"Creating RNodeInterface on {slave_path}...")
        active_ifac = RNodeInterface(RNS.Transport, ictx)
        active_ifac.IN = True
        active_ifac.OUT = True
        active_ifac.mode = Interface.MODE_FULL

        if active_ifac not in RNS.Transport.interfaces:
            RNS.Transport.interfaces.append(active_ifac)

        log(f"RNodeInterface online: {getattr(active_ifac, 'online', '?')}")
        return "ONLINE"

    except Exception as e:
        log(f"Injection Error: {e}")
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
        dest = RNS.Destination(dest_id, RNS.Destination.OUT, RNS.Destination.SINGLE, "lxmf", "delivery")
        if dest_id is None: dest.hash = dest_hash
        
        lxm = LXMessage(dest, local_destination, text)
        router.handle_outbound(lxm)
        return RNS.hexrep(lxm.hash, False)
    except Exception as e:
        log(f"Send Error: {e}")
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
