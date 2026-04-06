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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
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
        Log.i("RNS_SERVICE", "Service onCreate - Starting Foreground")
        initializePythonEnvironment()
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

        if (!Python.isStarted()) {
            Log.i("RNS_SERVICE", "Starting Python Engine...")
            Python.start(AndroidPlatform(this))
        }
        serviceScope.launch { 
            try {
                Log.i("RNS_SERVICE", "Initializing RNS Stack in Python...")
                Python.getInstance().getModule("rns_bridge").callAttr("start_rns", filesDir.absolutePath, this@RNodeService, "Harvester")
                onStatusUpdate("RNS Stack Initialized")
            } catch (e: Exception) {
                Log.e("RNS_SERVICE", "Python Start Error", e)
                onStatusUpdate("Python Error: ${e.message}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "INJECT_CONFIG") {
            injectPython()
            return START_STICKY
        }

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
                onStatusUpdate("Connecting to Bluetooth...")
                
                // If we're already bridging to this MAC, don't close everything
                if (mac == currentMac && isBridging && btSocket?.isConnected == true) {
                    Log.i("RNS_SERVICE", "Already connected to $mac, skipping bridge restart")
                    onStatusUpdate("Already Connected")
                    return@launch
                }

                // Aggressively close everything
                isBridging = false
                try {
                    btSocket?.inputStream?.close()
                    btSocket?.outputStream?.close()
                    btSocket?.close()
                } catch (e: Exception) {
                    Log.w("RNS_SERVICE", "Error closing old socket: ${e.message}")
                }
                btSocket = null
                tcpServer?.close()
                tcpServer = null
                delay(2000) // Longer delay to let BT stack settle
                
                val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = bluetoothManager.adapter
                if (adapter == null || !adapter.isEnabled) {
                    onStatusUpdate("Bluetooth is Disabled")
                    return@launch
                }
                val device = adapter.getRemoteDevice(mac)
                
                // Try Insecure UUID first (best for RNodes), then reflection as fallback
                var connected = false
                var retryCount = 0
                while (!connected && retryCount < 3) {
                    try {
                        Log.i("RNS_SERVICE", "Connection attempt ${retryCount + 1} to $mac")
                        onStatusUpdate("BT Attempt ${retryCount + 1}...")
                        
                        btSocket?.close()
                        delay(1000)
                        
                        // Use Insecure socket to avoid pairing/PIN issues
                        btSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                        delay(1000) // Give stack time to initialize
                        btSocket?.connect()
                        connected = true
                        Log.i("RNS_SERVICE", "Bluetooth Socket Connected Successfully")
                    } catch (e: Exception) {
                        Log.e("RNS_SERVICE", "BT Connection Attempt ${retryCount + 1} Failed: ${e.message}")
                        retryCount++
                        if (retryCount < 3) {
                            onStatusUpdate("Retrying BT in 3s...")
                            delay(3000)
                        }
                    }
                }
                
                if (!connected) {
                    onStatusUpdate("BT Connection Failed")
                    throw Exception("Failed to connect to RNode after retries")
                }

                Log.i("RNS_SERVICE", "BT Connected. Starting TCP Server on 7633...")
                onStatusUpdate("BT Connected. Starting TCP...")
                isBridging = true
                currentMac = mac
                getSharedPreferences("rns_database", Context.MODE_PRIVATE).edit().putString("last_mac", mac).apply()
                
                Log.i("RNS_SERVICE", "Launching handleTcpClients coroutine...")
                launch { handleTcpClients() }

            } catch (e: Exception) { 
                currentMac = "" 
                isBridging = false
                Log.e("RNS_SERVICE", "BT Bridge failed", e)
                onStatusUpdate("Error: ${e.message}")
            }
        }
    }

    private suspend fun handleTcpClients() {
        withContext(Dispatchers.IO) {
            try {
                tcpServer = ServerSocket()
                tcpServer?.reuseAddress = true
                tcpServer?.soTimeout = 30000 // 30 second timeout for accept
                tcpServer?.bind(InetSocketAddress("127.0.0.1", 7633))
                Log.i("RNS_BRIDGE", "TCP Bridge Server listening on 127.0.0.1:7633")
                
                while (isBridging && isActive) {
                    try {
                        Log.i("RNS_SERVICE", "TCP Server: Calling accept() - waiting for Python...")
                        onStatusUpdate("Waiting for Python Bridge...")
                        
                        // We trigger Python injection JUST BEFORE accept to minimize race window
                        // but Python will retry if it beats us.
                        launch {
                            delay(500)
                            Log.i("RNS_SERVICE", "Triggering injectPython()...")
                            injectPython()
                        }

                        val client = tcpServer?.accept() ?: break
                        client.tcpNoDelay = true
                        Log.i("RNS_SERVICE", "TCP Server: Python client connected from ${client.inetAddress}")
                        onStatusUpdate("RNode Bridge Connected")
                        
                        val btIn = btSocket!!.inputStream
                        val btOut = btSocket!!.outputStream
                        val tcpIn = client.inputStream
                        val tcpOut = client.outputStream

                        coroutineScope {
                            launch {
                                try {
                                    val buf = ByteArray(2048)
                                    var r = 0
                                    while (isBridging && isActive) {
                                        r = btIn.read(buf)
                                        if (r == -1) break
                                        if (r > 0) { 
                                            Log.v("RNS_BRIDGE", "BT -> TCP: $r bytes")
                                            tcpOut.write(buf, 0, r)
                                            tcpOut.flush() 
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("RNS_SERVICE", "BT Read Error", e)
                                } finally {
                                    Log.w("RNS_SERVICE", "BT Link Lost - Resetting Bridge")
                                    isBridging = false
                                    currentMac = ""
                                    onStatusUpdate("Connection Lost")
                                    try { client.close() } catch (e: Exception) { }
                                    try { btSocket?.close() } catch (e: Exception) { }
                                    try { tcpServer?.close() } catch (e: Exception) { }
                                }
                            }

                            launch {
                                try {
                                    val buf = ByteArray(2048)
                                    var r = 0
                                    while (isBridging && isActive) {
                                        r = tcpIn.read(buf)
                                        if (r == -1) break
                                        if (r > 0) { 
                                            Log.v("RNS_BRIDGE", "TCP -> BT: $r bytes")
                                            btOut.write(buf, 0, r)
                                            btOut.flush() 
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("RNS_SERVICE", "TCP Read Error", e)
                                } finally {
                                    try { client.close() } catch (e: Exception) { }
                                }
                            }
                        }
                    } catch (e: Exception) { 
                        Log.e("RNS_SERVICE", "TCP Accept Error", e)
                        break 
                    }
                }
            } catch (e: Exception) {
                Log.e("RNS_SERVICE", "TCP Server Error", e)
            } finally {
                try { tcpServer?.close() } catch (e: Exception) { }
                tcpServer = null
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
        emitStatus(msg)
    }

    private fun initializePythonEnvironment() {
        try {
            // Ensure Python is properly initialized
            val pythonPath = filesDir.absolutePath + "/python"
            val cachePath = cacheDir.absolutePath
            
            // Set required environment variables
            System.setProperty("python.home", pythonPath)
            System.setProperty("python.path", pythonPath)
            
            Log.d("RNS_SERVICE", "Python environment initialized: $pythonPath")
        } catch (e: Exception) {
            Log.e("RNS_SERVICE", "Failed to initialize Python environment", e)
        }
    }

    companion object {
        val statusFlow = MutableSharedFlow<String>(
            replay = 1,
            extraBufferCapacity = 10,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        fun emitStatus(msg: String) {
            statusFlow.tryEmit(msg)
        }
    }
}
