package com.palmharvest.pro

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Build
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

@CapacitorPlugin(name = "RNSPlugin")
class RNSPlugin : Plugin() {
    companion object {
        private var instance: RNSPlugin? = null
        
        fun onStatusUpdate(message: String) {
            val ret = JSObject()
            ret.put("message", message)
            instance?.notifyListeners("onStatusUpdate", ret)
        }
    }

    private var python: Python? = null
    private var bridgeThread: Thread? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var serverSocket: ServerSocket? = null
    private var tcpSocket: Socket? = null

    override fun load() {
        instance = this
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        python = Python.getInstance()
    }

    @PluginMethod
    fun startRNS(call: PluginCall) {
        val nickname = call.getString("nickname") ?: "PalmHarvest-User"
        val storagePath = context.filesDir.absolutePath
        
        try {
            val bridge = python?.getModule("rns_bridge")
            val localHash = bridge?.callAttr("start_rns", storagePath, RNSCallback(), nickname)?.toString()
            
            val ret = JSObject()
            ret.put("localHash", localHash)
            call.resolve(ret)
        } catch (e: Exception) {
            call.reject(e.message)
        }
    }

    @PluginMethod
    fun getPairedDevices(call: PluginCall) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val adapter = bluetoothManager.adapter
        
        val devices = JSObject()
        val deviceList = com.getcapacitor.JSArray()
        
        if (adapter != null && adapter.isEnabled) {
            adapter.bondedDevices.forEach { device ->
                val d = JSObject()
                d.put("name", device.name ?: "Unknown")
                d.put("address", device.address)
                deviceList.put(d)
            }
        }
        
        devices.put("devices", deviceList)
        call.resolve(devices)
    }

    @PluginMethod
    fun connectRNode(call: PluginCall) {
        val address = call.getString("address")
        if (address == null) {
            call.reject("Address is required")
            return
        }

        try {
            val intent = Intent(context, RNodeService::class.java)
            intent.putExtra("mac", address)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            call.resolve()
        } catch (e: Exception) {
            call.reject(e.message)
        }
    }

    @PluginMethod
    fun disconnectRNode(call: PluginCall) {
        try {
            val intent = Intent(context, RNodeService::class.java)
            context.stopService(intent)
            call.resolve()
        } catch (e: Exception) {
            call.reject(e.message)
        }
    }

    private fun startBridge() {
        bridgeThread = thread {
            try {
                serverSocket = ServerSocket(7633)
                tcpSocket = serverSocket?.accept()
                
                val btIn = bluetoothSocket?.inputStream
                val btOut = bluetoothSocket?.outputStream
                val tcpIn = tcpSocket?.inputStream
                val tcpOut = tcpSocket?.outputStream

                // BT -> TCP
                thread {
                    val buffer = ByteArray(1024)
                    try {
                        while (true) {
                            val bytes = btIn?.read(buffer) ?: -1
                            if (bytes > 0) {
                                tcpOut?.write(buffer, 0, bytes)
                                tcpOut?.flush()
                            }
                        }
                    } catch (e: Exception) {}
                }

                // TCP -> BT
                thread {
                    val buffer = ByteArray(1024)
                    try {
                        while (true) {
                            val bytes = tcpIn?.read(buffer) ?: -1
                            if (bytes > 0) {
                                btOut?.write(buffer, 0, bytes)
                                btOut?.flush()
                            }
                        }
                    } catch (e: Exception) {}
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @PluginMethod
    fun injectRNode(call: PluginCall) {
        val freq = call.getInt("frequency") ?: 433000000
        val bw = call.getInt("bandwidth") ?: 125000
        val tx = call.getInt("txpower") ?: 17
        val sf = call.getInt("spreadingfactor") ?: 8
        val cr = call.getInt("codingrate") ?: 6

        try {
            val bridge = python?.getModule("rns_bridge")
            val status = bridge?.callAttr("inject_rnode", freq, bw, tx, sf, cr)?.toString()
            
            val ret = JSObject()
            ret.put("status", status)
            call.resolve(ret)
        } catch (e: Exception) {
            call.reject(e.message)
        }
    }

    @PluginMethod
    fun sendText(call: PluginCall) {
        val dest = call.getString("destination")
        val text = call.getString("text")
        
        if (dest == null || text == null) {
            call.reject("Destination and text are required")
            return
        }

        try {
            val bridge = python?.getModule("rns_bridge")
            val msgHash = bridge?.callAttr("send_text", dest, text)?.toString()
            
            val ret = JSObject()
            ret.put("messageHash", msgHash)
            call.resolve(ret)
        } catch (e: Exception) {
            call.reject(e.message)
        }
    }

    @PluginMethod
    fun announce(call: PluginCall) {
        try {
            val bridge = python?.getModule("rns_bridge")
            bridge?.callAttr("announce_now")
            call.resolve()
        } catch (e: Exception) {
            call.reject(e.message)
        }
    }

    inner class RNSCallback {
        fun onAnnounceReceived(hash: String, name: String) {
            val ret = JSObject()
            ret.put("hash", hash)
            ret.put("name", name)
            notifyListeners("onAnnounceReceived", ret)
        }

        fun onNewMessage(sender: String, content: String, time: Long, isImg: Boolean, isAck: Boolean, hash: String) {
            val ret = JSObject()
            ret.put("sender", sender)
            ret.put("content", content)
            ret.put("time", time)
            ret.put("isImg", isImg)
            ret.put("isAck", isAck)
            ret.put("hash", hash)
            notifyListeners("onNewMessage", ret)
        }

        fun onMessageDelivered(hash: String) {
            val ret = JSObject()
            ret.put("hash", hash)
            notifyListeners("onMessageDelivered", ret)
        }
    }
}
