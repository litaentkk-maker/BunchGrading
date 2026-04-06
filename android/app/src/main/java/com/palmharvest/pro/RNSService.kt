package com.palmharvest.pro

import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

data class RNSStatus(
    val isConnected: Boolean = false,
    val isRnsRunning: Boolean = false,
    val localHash: String? = null,
    val deviceName: String? = null,
    val statusMessage: String = "Idle"
)

class RNSService(private val context: Context) {

    private var python: Python? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _status = MutableStateFlow(RNSStatus())
    val status: StateFlow<RNSStatus> = _status

    init {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        python = Python.getInstance()
        
        // Collect status updates from RNodeService
        serviceScope.launch {
            RNodeService.statusFlow.collect { msg ->
                _status.value = _status.value.copy(statusMessage = msg)
                
                // Update specific status flags based on message content
                if (msg.contains("RNode Bridge Connected")) {
                    _status.value = _status.value.copy(isConnected = true)
                } else if (msg.contains("RNS Stack Initialized")) {
                    _status.value = _status.value.copy(isRnsRunning = true)
                } else if (msg.contains("Connection Lost") || msg.contains("BT Connection Failed")) {
                    _status.value = _status.value.copy(isConnected = false)
                }
            }
        }
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
        val intent = Intent(context, RNodeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun connectRNode(address: String) {
        val intent = Intent(context, RNodeService::class.java)
        intent.putExtra("mac", address)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun disconnectRNode() {
        context.stopService(Intent(context, RNodeService::class.java))
        _status.value = _status.value.copy(isConnected = false, deviceName = "")
    }

    fun injectRNode(freq: Int, bw: Int, tx: Int, sf: Int, cr: Int) {
        // Save to prefs first
        val prefs = context.getSharedPreferences("rns_database", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("freq", freq)
            putInt("bw", bw)
            putInt("tx", tx)
            putInt("sf", sf)
            putInt("cr", cr)
            apply()
        }
        
        // Trigger injection in service
        val intent = Intent(context, RNodeService::class.java)
        intent.action = "INJECT_CONFIG"
        context.startService(intent)
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
