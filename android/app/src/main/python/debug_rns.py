import RNS.Interfaces.RNodeInterface as rni
import inspect
try:
    print(inspect.getsource(rni.RNodeInterface.__init__))
except Exception as e:
    print(f"Error: {e}")
