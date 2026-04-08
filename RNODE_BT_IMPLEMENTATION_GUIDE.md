# RNode Bluetooth Implementation Guide

This document outlines the exact steps required to replace the mock `rnsService.ts` with a fully functional Bluetooth connection to an RNode (Reticulum Network Stack) on Android using Capacitor.

## 1. Architecture Overview
The application runs in a Capacitor WebView. To communicate with an RNode via Bluetooth, we must use native Android Bluetooth APIs exposed through a Capacitor plugin.
* **Hardware:** RNode (typically ESP32-based) exposing a Bluetooth Serial Port Profile (SPP) or BLE UART.
* **Plugin:** `capacitor-bluetooth-serial` (for Bluetooth Classic SPP) or `@capacitor-community/bluetooth-le` (for BLE). *Note: Most standard RNodes use Bluetooth Classic SPP.*
* **Protocol:** RNodes expect data wrapped in the **KISS (Keep It Simple, Stupid)** framing protocol over the serial link.

## 2. Android Native Permissions (AndroidManifest.xml)
The next agent must add the following permissions to `android/app/src/main/AndroidManifest.xml`:

```xml
<!-- Legacy Bluetooth -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<!-- Android 12+ Bluetooth -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<!-- Location (Required for scanning on older Android versions) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

## 3. Capacitor Plugin Implementation Steps
Update `src/services/rnsService.ts` to use `capacitor-bluetooth-serial`.

### A. Scanning and Connecting
1. **Enable Bluetooth:** Check if Bluetooth is enabled using the plugin. Prompt the user to enable it if not.
2. **List Paired Devices:** RNodes are usually paired via the Android OS settings first. Use `BluetoothSerial.list()` to get paired devices.
3. **Filter:** Look for devices with names containing "RNode" or a specific MAC address.
4. **Connect:** Call `BluetoothSerial.connect({ address: macAddress })`.

### B. The KISS Protocol (CRITICAL)
RNodes do not accept raw text. They act as KISS TNCs. The next agent MUST implement a KISS framer/deframer in TypeScript.

**KISS Constants:**
* `FEND` (Frame End): `0xC0`
* `FESC` (Frame Escape): `0xDB`
* `TFEND` (Transposed Frame End): `0xDC`
* `TFESC` (Transposed Frame Escape): `0xDD`
* `CMD_DATA`: `0x00`

**Sending Data (Framing):**
1. Start with `FEND`.
2. Add Command Byte (`0x00` for data).
3. Iterate through payload bytes:
   * If byte is `FEND`, send `FESC` then `TFEND`.
   * If byte is `FESC`, send `FESC` then `TFESC`.
   * Otherwise, send byte.
4. End with `FEND`.
5. Convert to Base64 or Uint8Array (depending on plugin requirements) and send via `BluetoothSerial.write()`.

**Receiving Data (Deframing):**
1. Subscribe to `BluetoothSerial.addListener('rawData', callback)`.
2. Buffer incoming bytes.
3. Extract frames bounded by `FEND` (`0xC0`).
4. Unescape `FESC`+`TFEND` back to `FEND`, and `FESC`+`TFESC` back to `FESC`.
5. Strip the command byte. The remaining payload is the Reticulum packet.

## 4. Reticulum Packet Formatting
Once the KISS framing is working, the data sent inside the KISS frame must be a valid Reticulum packet (IFAC, Announce, or Data packet).
* If the app is just acting as a dumb terminal, it needs to construct LXMF (Lightweight Extensible Message Format) packets.
* **Recommendation for next agent:** Implement a lightweight TypeScript Reticulum packet builder that constructs the binary headers (Destination Hash, Context, Type) required by the Reticulum network, or interface with a local Python `rnsd` instance if running on a full OS. For Android, building the binary packets in TS is required.

## 5. State Management (`rnsService.ts`)
The service should maintain the following state and expose it via listeners to the React UI:
```typescript
interface RNSState {
  isConnected: boolean;
  isScanning: boolean;
  connectedDevice: string | null;
  statusMessage: string;
  bytesSent: number;
  bytesReceived: number;
}
```
* Implement automatic reconnection logic (retry every 5 seconds if connection drops).
* Emit state changes so the `App.tsx` diagnostics overlay updates in real-time.

## Summary Checklist for Next Agent
- [ ] Verify `capacitor-bluetooth-serial` is installed and synced.
- [ ] Update `AndroidManifest.xml` with Android 12+ Bluetooth permissions.
- [ ] Implement `connect()` logic in `rnsService.ts` using the plugin.
- [ ] Implement KISS framing/deframing utility functions.
- [ ] Implement Reticulum binary packet construction for Harvest Records.
- [ ] Add a "Select Device" UI in `RNSPage.tsx` to let the user pick their paired RNode.
