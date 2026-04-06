package com.palmharvest.pro

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.net.InetSocketAddress
import java.net.ServerSocket
import android.util.Log
import android.os.Build
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.UUID

class RNodeService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var btSocket: BluetoothSocket? = null
    private var tcpServer: ServerSocket? = null
    private var isBridging = false
    private var currentMac = ""
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(this, "RNS_CHANNEL")
            .setContentTitle("PalmHarvest Mesh Active")
            .setContentText("Maintaining RNode Connection")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }

        if (!Python.isStarted()) Python.start(AndroidPlatform(this))
        serviceScope.launch { 
            try {
                Python.getInstance().getModule("rns_bridge").callAttr("start_rns", filesDir.absolutePath, this@RNodeService, "Harvester")
            } catch (e: Exception) {
                Log.e("RNS_SERVICE", "Python Start Error", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mac = intent?.getStringExtra("mac") ?: getSharedPreferences("rns_database", Context.MODE_PRIVATE).getString("last_mac", "") ?: ""
        
        // Only start bridge if MAC is different OR if bridge is not active
        if (mac.isNotEmpty() && (mac != currentMac || !isBridging || btSocket?.isConnected != true)) {
            startBridge(mac)
        }
        return START_STICKY
    }

    private fun startBridge(mac: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                Log.i("RNS_SERVICE", "Connecting BT to $mac...")
                
                // If we're already bridging to this MAC, don't close everything
                if (mac == currentMac && isBridging && btSocket?.isConnected == true) {
                    Log.i("RNS_SERVICE", "Already connected to $mac, skipping bridge restart")
                    return@launch
                }

                isBridging = false
                btSocket?.close()
                tcpServer?.close()
                delay(1000)
                
                val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = bluetoothManager.adapter
                val device = adapter.getRemoteDevice(mac)
                
                // Try UUID first, then reflection as fallback
                var connected = false
                var retryCount = 0
                while (!connected && retryCount < 2) {
                    try {
                        Log.i("RNS_SERVICE", "Connection attempt ${retryCount + 1}")
                        btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                        btSocket?.connect()
                        connected = true
                    } catch (e: Exception) {
                        Log.w("RNS_SERVICE", "UUID connection failed, trying reflection...")
                        try {
                            val m = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                            btSocket = m.invoke(device, 1) as BluetoothSocket
                            btSocket?.connect()
                            connected = true
                        } catch (e2: Exception) {
                            Log.e("RNS_SERVICE", "Reflection connection failed too", e2)
                            retryCount++
                            if (retryCount < 2) delay(2000)
                        }
                    }
                }
                
                if (!connected) throw Exception("Failed to connect to RNode after retries")

                tcpServer = ServerSocket()
                tcpServer?.reuseAddress = true
                tcpServer?.bind(InetSocketAddress("127.0.0.1", 7633))
                
                isBridging = true
                currentMac = mac
                getSharedPreferences("rns_database", Context.MODE_PRIVATE).edit().putString("last_mac", mac).apply()
                
                Log.i("RNS_SERVICE", "Bridge 7633 Ready for $mac")

                launch { handleTcpClients() }
                delay(1000)
                injectPython()

            } catch (e: Exception) { 
                currentMac = "" 
                isBridging = false
                Log.e("RNS_SERVICE", "BT Bridge failed", e)
            }
        }
    }

    private suspend fun handleTcpClients() {
        withContext(Dispatchers.IO) {
            while (isBridging && isActive) {
                try {
                    val client = tcpServer?.accept() ?: break
                    client.tcpNoDelay = true
                    Log.i("RNS_SERVICE", "LoRa Link Active (TCP Client connected)")

                    val btIn = btSocket!!.inputStream
                    val btOut = btSocket!!.outputStream
                    val tcpIn = client.inputStream
                    val tcpOut = client.outputStream

                    launch {
                        try {
                            val buf = ByteArray(2048)
                            var r = 0
                            while (isBridging && isActive && btIn.read(buf).also { r = it } != -1) {
                                if (r > 0) { tcpOut.write(buf, 0, r); tcpOut.flush() }
                            }
                        } catch (e: Exception) { }
                        finally { try { client.close() } catch (e: Exception) { } }
                    }

                    launch {
                        try {
                            val buf = ByteArray(2048)
                            var r = 0
                            while (isBridging && isActive && tcpIn.read(buf).also { r = it } != -1) {
                                if (r > 0) { btOut.write(buf, 0, r); btOut.flush() }
                            }
                        } catch (e: Exception) { }
                        finally { try { client.close() } catch (e: Exception) { } }
                    }
                } catch (e: Exception) { break }
            }
        }
    }

    private fun injectPython() {
        serviceScope.launch {
            try {
                val py = Python.getInstance()
                // Get params from shared prefs or use defaults
                val prefs = getSharedPreferences("rns_database", Context.MODE_PRIVATE)
                val json = JSONObject()
                json.put("freq", prefs.getInt("freq", 433000000))
                json.put("sf", prefs.getInt("sf", 8))
                json.put("cr", prefs.getInt("cr", 6))
                json.put("tx", prefs.getInt("tx", 17))
                json.put("bw", prefs.getInt("bw", 125000))
                
                py.getModule("rns_bridge").callAttr("inject_rnode_json", json.toString())
            } catch (e: Exception) {
                Log.e("RNS_SERVICE", "Python Injection Error", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("RNS_CHANNEL", "Mesh Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() { 
        isBridging = false
        serviceScope.cancel()
        btSocket?.close()
        tcpServer?.close()
        super.onDestroy() 
    }

    // Python Callbacks
    fun onAnnounceReceived(hash: String, name: String) {
        Log.i("RNS_SERVICE", "Announce: $hash ($name)")
    }
    
    fun onNewMessage(sender: String, content: String, ts: Long, isImg: Boolean, isAck: Boolean, msgHash: String) {
        Log.i("RNS_SERVICE", "New Message from $sender")
    }

    fun onMessageDelivered(msgHash: String) {
        Log.i("RNS_SERVICE", "Message Delivered: $msgHash")
    }

    fun onStatusUpdate(msg: String) {
        Log.i("RNS_SERVICE", "Status: $msg")
    }
}
