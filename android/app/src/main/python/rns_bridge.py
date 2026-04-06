import os, sys, time, base64, signal, warnings, json
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

def start_rns(storage_path, callback_obj, nickname):
    global router, local_destination, kotlin_callback, is_rns_running
    kotlin_callback = callback_obj
    if is_rns_running and local_destination is not None: 
        return RNS.hexrep(local_destination.hash, False)
    
    try:
        storage_path = str(storage_path)
        os.environ["TMPDIR"] = os.path.join(storage_path, "cache")
        rns_dir = os.path.join(storage_path, ".reticulum")
        lxmf_dir = os.path.join(storage_path, ".lxmf")
        
        # Ensure all directories exist with proper permissions
        for d in [os.environ["TMPDIR"], rns_dir, lxmf_dir]:
            if not os.path.exists(d):
                os.makedirs(d, mode=0o755)
        
        # Create config if missing
        config_path = os.path.join(rns_dir, "config")
        if not os.path.exists(config_path):
            with open(config_path, "w") as f:
                f.write("[reticulum]\nenable_transport = True\nshare_instance = Yes\n\n[interfaces]\n")
        
        # Initialize Reticulum
        RNS.Reticulum(configdir=rns_dir)
        
        # Load or create identity
        id_path = os.path.join(storage_path, "storage_identity")
        local_id = RNS.Identity.from_file(id_path) if os.path.exists(id_path) else RNS.Identity()
        if not os.path.exists(id_path):
            local_id.to_file(id_path)
        
        # Setup router
        router = LXMRouter(identity=local_id, storagepath=lxmf_dir)
        local_destination = router.register_delivery_identity(local_id, display_name=nickname)
        router.register_delivery_callback(on_lxmf)
        RNS.Transport.register_announce_handler(discovery_handler())
        is_rns_running = True
        
        log(f"RNS STARTED: {RNS.hexrep(local_destination.hash, False)}")
        return RNS.hexrep(local_destination.hash, False)
    except Exception as e:
        log(f"FATAL ERROR starting RNS: {e}")
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
    log(f"TUNING: F:{freq} BW:{bw} SF:{sf} CR:{cr}")
    try:
        # Remove old interface if it exists
        if active_ifac is not None:
            try:
                log("Removing old interface...")
                if active_ifac in RNS.Transport.interfaces:
                    RNS.Transport.interfaces.remove(active_ifac)
                active_ifac.detach()
            except Exception as e:
                log(f"Error removing old interface: {e}")

        # Use the standard RNodeInterface and point it to our local TCP bridge
        # We provide both 'port' and 'tcp_host/port' for maximum compatibility across RNS versions
        ictx = {
            "name": "Bridge", 
            "type": "RNodeInterface", 
            "interface_enabled": True, 
            "outgoing": True,
            "port": "tcp://127.0.0.1:7633",
            "tcp_host": "127.0.0.1",
            "tcp_port": 7633,
            "frequency": int(freq), 
            "bandwidth": int(bw),
            "txpower": int(tx), 
            "spreadingfactor": int(sf), 
            "codingrate": int(cr), 
            "flow_control": False
        }
        log(f"Injecting RNode interface via TCP:127.0.0.1:7633")
        
        # We use a small delay to ensure the Kotlin TCP server is ready
        time.sleep(0.5)
        
        active_ifac = RNodeInterface(RNS.Transport, ictx)
        active_ifac.mode = Interface.MODE_FULL
        active_ifac.IN = True; active_ifac.OUT = True
        
        # Manually add to transport if not already there
        if active_ifac not in RNS.Transport.interfaces:
            RNS.Transport.interfaces.append(active_ifac)
            
        log(f"Interface Injection Done. Status: {active_ifac}")
        
        # Check if it's actually connected
        if hasattr(active_ifac, "online") and not active_ifac.online:
            log("Warning: Interface is offline. Checking bridge...")
            
        return "ONLINE"
    except Exception as e: 
        log(f"Injection Error: {str(e)}")
        import traceback
        log(traceback.format_exc())
        return str(e)

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
