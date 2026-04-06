package com.palmharvest.pro

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class RNSStatus(
    val isConnected: Boolean = false,
    val isRnsRunning: Boolean = false,
    val localHash: String? = null,
    val deviceName: String? = null
)

class RNSService(private val context: Context) {

    private var python: Python? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var serverSocket: ServerSocket? = null
    private var tcpSocket: Socket? = null

    private val _status = MutableStateFlow(RNSStatus())
    val status: StateFlow<RNSStatus> = _status

    init {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        python = Python.getInstance()
    }

    fun getPairedDevices(): List<Pair<String, String>> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        return if (adapter != null && adapter.isEnabled) {
            adapter.bondedDevices.map { it.name to it.address }
        } else {
            emptyList()
        }
    }

    fun startRNS(nickname: String) {
        val storagePath = context.filesDir.absolutePath
        thread {
            try {
                val bridge = python?.getModule("rns_bridge")
                val localHash = bridge?.callAttr("start_rns", storagePath, RNSCallback(), nickname)?.toString()
                _status.value = _status.value.copy(isRnsRunning = true, localHash = localHash)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun connectRNode(address: String) {
        thread {
            try {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = bluetoothManager.adapter
                if (adapter == null) {
                    showToast("Bluetooth not supported on this device")
                    return@thread
                }
                if (!adapter.isEnabled) {
                    showToast("Please enable Bluetooth")
                    return@thread
                }
                
                val device: BluetoothDevice = adapter.getRemoteDevice(address)
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothSocket?.connect()
                
                _status.value = _status.value.copy(isConnected = true, deviceName = device.name ?: "RNode")
                startBridge()
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Connection failed: ${e.message}")
                _status.value = _status.value.copy(isConnected = false)
            }
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun disconnectRNode() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
            serverSocket?.close()
            serverSocket = null
            tcpSocket?.close()
            tcpSocket = null
            _status.value = _status.value.copy(isConnected = false, deviceName = "")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startBridge() {
        thread {
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

    fun injectRNode(freq: Int, bw: Int, tx: Int, sf: Int, cr: Int) {
        thread {
            try {
                val bridge = python?.getModule("rns_bridge")
                bridge?.callAttr("inject_rnode", freq, bw, tx, sf, cr)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendText(dest: String, text: String) {
        thread {
            try {
                val bridge = python?.getModule("rns_bridge")
                bridge?.callAttr("send_text", dest, text)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun announce() {
        thread {
            try {
                val bridge = python?.getModule("rns_bridge")
                bridge?.callAttr("announce_now")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    inner class RNSCallback {
        fun onAnnounceReceived(hash: String, name: String) {
            // Handle announce
        }

        fun onNewMessage(sender: String, content: String, time: Long, isImg: Boolean, isAck: Boolean, hash: String) {
            // Handle message
        }

        fun onMessageDelivered(hash: String) {
            // Handle delivery
        }
    }
}
