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

class RNodeService : Service() {
    private var btSocket: BluetoothSocket? = null
    private var tcpServer: ServerSocket? = null
    private var isBridging = false
    private var currentMac = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "RNS_CHANNEL")
            .setContentTitle("PalmHarvest Mesh Active")
            .setContentText("Maintaining RNode Connection")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        // For Android 14+ we need to specify foreground service type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }
        
        val mac = intent?.getStringExtra("mac") ?: getSharedPreferences("rns_database", Context.MODE_PRIVATE).getString("last_mac", "") ?: ""
        if (mac.isNotEmpty() && mac != currentMac) {
            startBridge(mac)
        }
        return START_STICKY
    }

    private fun startBridge(mac: String) {
        Thread {
            try {
                isBridging = false
                btSocket?.close()
                tcpServer?.close()
                Thread.sleep(1000)
                
                val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = bluetoothManager.adapter
                val device = adapter.getRemoteDevice(mac)
                
                // Use the insecure RFCOMM socket method as requested by user
                val m = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                btSocket = m.invoke(device, 1) as BluetoothSocket
                btSocket?.connect()
                
                tcpServer = ServerSocket()
                tcpServer?.reuseAddress = true
                tcpServer?.bind(InetSocketAddress("127.0.0.1", 7633))
                
                isBridging = true
                currentMac = mac
                
                // Save last MAC
                getSharedPreferences("rns_database", Context.MODE_PRIVATE).edit().putString("last_mac", mac).apply()
                
                while(isBridging) {
                    val client = tcpServer?.accept() ?: break
                    val btIn = btSocket?.inputStream; val btOut = btSocket?.outputStream
                    val tcpIn = client.inputStream; val tcpOut = client.outputStream
                    
                    // TCP -> BT
                    Thread { 
                        try { 
                            val buf = ByteArray(1024)
                            var r=0
                            while(isBridging && tcpIn.read(buf).also{r=it}!=-1) {
                                btOut?.write(buf,0,r)
                                btOut?.flush()
                            }
                        } catch(e:Exception){
                            Log.e("RNS_SERVICE", "TCP to BT failed", e)
                        } 
                    }.start()
                    
                    // BT -> TCP
                    Thread { 
                        try { 
                            val buf = ByteArray(1024)
                            var r=0
                            while(isBridging && btIn!!.read(buf).also{r=it}!=-1) {
                                tcpOut.write(buf,0,r)
                                tcpOut.flush()
                            } 
                        } catch(e:Exception){
                            Log.e("RNS_SERVICE", "BT to TCP failed", e)
                        } finally {
                            client.close()
                        } 
                    }.start()
                }
            } catch (e: Exception) { 
                currentMac = "" 
                Log.e("RNS_SERVICE", "BT Bridge failed", e)
                // Notify user or retry?
            }
        }.start()
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
        btSocket?.close()
        tcpServer?.close()
        super.onDestroy() 
    }
}
