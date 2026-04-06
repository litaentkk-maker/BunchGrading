import serial

def _patched_serial(*args, **kwargs):
    port = kwargs.get('port') or (args[0] if args else None)
    if port and str(port).startswith("socket://"):
        if 'port' in kwargs:
            del kwargs['port']
        new_args = args[1:] if args else ()
        return serial.serial_for_url(port, *new_args, **kwargs)
    return serial.Serial(*args, **kwargs)

try:
    s = _patched_serial(port="socket://127.0.0.1:7633")
    print("Success with kwarg")
except Exception as e:
    print("Error with kwarg:", e)

try:
    s = _patched_serial("socket://127.0.0.1:7633")
    print("Success with arg")
except Exception as e:
    print("Error with arg:", e)
