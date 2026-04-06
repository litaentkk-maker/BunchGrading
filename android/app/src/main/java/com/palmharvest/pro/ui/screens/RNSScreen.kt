package com.palmharvest.pro.ui.screens

import com.palmharvest.pro.RNSService
import com.palmharvest.pro.RNSStatus
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palmharvest.pro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RNSScreen(
    rnsService: RNSService,
    onBack: () -> Unit = {}
) {
    val rnsStatus by rnsService.status.collectAsState()
    val isConnected = rnsStatus.isConnected
    val isRnsRunning = rnsStatus.isRnsRunning
    val localHash = rnsStatus.localHash
    val deviceName = rnsStatus.deviceName
    
    var isSyncing by remember { mutableStateOf(false) }
    var syncProgress by remember { mutableStateOf(0) }
    var showDevicePicker by remember { mutableStateOf(false) }
    val pairedDevices = remember { mutableStateListOf<Pair<String, String>>() }

    var frequency by remember { mutableStateOf("433000000") }
    var bandwidth by remember { mutableStateOf("125000") }
    var txPower by remember { mutableStateOf("17") }
    var spreadingFactor by remember { mutableStateOf("8") }
    var codingRate by remember { mutableStateOf("6") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray50)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(White)
                    .padding(4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Gray600)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "RNS Bridge",
                    style = MaterialTheme.typography.displaySmall,
                    color = Gray900
                )
                Text(
                    text = "Off-grid LoRa Sync",
                    style = MaterialTheme.typography.labelMedium,
                    color = Gray500
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Live Diagnostics Box (Always Visible)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LIVE SYSTEM DIAGNOSTICS", style = MaterialTheme.typography.labelSmall, color = Color.Green, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = rnsStatus.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Green,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Connection Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (rnsStatus.isConnected) Color(0xFFF0FDF4) else Gray50
                        ) {
                            Icon(
                                imageVector = if (rnsStatus.isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                                contentDescription = null,
                                tint = if (rnsStatus.isConnected) Color(0xFF16A34A) else Gray400,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("RNode Connection", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text(
                                if (rnsStatus.isConnected) "Connected to ${rnsStatus.deviceName}" else "No RNode connected",
                                style = MaterialTheme.typography.labelMedium,
                                color = Gray500
                            )
                        }
                    }
                    Button(
                        onClick = { 
                            if (!rnsStatus.isConnected) {
                                pairedDevices.clear()
                                pairedDevices.addAll(rnsService.getPairedDevices())
                                showDevicePicker = true
                            } else {
                                rnsService.disconnectRNode()
                            }
                        },
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (rnsStatus.isConnected) White else Primary600,
                            contentColor = if (rnsStatus.isConnected) Color(0xFFDC2626) else White
                        ),
                        border = if (rnsStatus.isConnected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEE2E2)) else null
                    ) {
                        Text(if (rnsStatus.isConnected) "Disconnect" else "Connect BT", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (rnsStatus.isConnected) {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Gray100)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (rnsStatus.isRnsRunning) Color(0xFF22C55E) else Gray300)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RNS STACK", style = MaterialTheme.typography.labelLarge, color = Gray700)
                        }
                        Button(
                            onClick = { rnsService.startRNS("PalmHarvest-User") },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gray900),
                            enabled = !rnsStatus.isRnsRunning
                        ) {
                            Text(if (rnsStatus.isRnsRunning) "RUNNING" else "START STACK", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    
                    if (rnsStatus.localHash != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Local Hash: ${rnsStatus.localHash}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Gray400,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // RNode Tuning Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFF7ED)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.padding(8.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("RNode Tuning", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text("LoRa Physical Layer Config", style = MaterialTheme.typography.labelMedium, color = Gray500)
                        }
                    }
                    
                    TextButton(
                        onClick = {
                            frequency = "433000000"
                            bandwidth = "125000"
                            txPower = "17"
                            spreadingFactor = "8"
                            codingRate = "6"
                        }
                    ) {
                        Text("RESET DEFAULTS", style = MaterialTheme.typography.labelSmall, color = Primary600, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Frequency (Hz)", style = MaterialTheme.typography.labelLarge, color = Gray500)
                        Text("${(frequency.toDoubleOrNull() ?: 0.0) / 1000000.0} MHz", style = MaterialTheme.typography.labelLarge, color = Primary600, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary600,
                            unfocusedBorderColor = Gray100
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bandwidth (Hz)", style = MaterialTheme.typography.labelMedium, color = Gray500)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = bandwidth,
                            onValueChange = { bandwidth = it },
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary600, unfocusedBorderColor = Gray100)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TX Power (dBm)", style = MaterialTheme.typography.labelMedium, color = Gray500)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = txPower,
                            onValueChange = { txPower = it },
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary600, unfocusedBorderColor = Gray100)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Spreading Factor", style = MaterialTheme.typography.labelMedium, color = Gray500)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = spreadingFactor,
                            onValueChange = { spreadingFactor = it },
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary600, unfocusedBorderColor = Gray100)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Coding Rate", style = MaterialTheme.typography.labelMedium, color = Gray500)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = codingRate,
                            onValueChange = { codingRate = it },
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary600, unfocusedBorderColor = Gray100)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        val f = frequency.toIntOrNull() ?: 433000000
                        val b = bandwidth.toIntOrNull() ?: 125000
                        val t = txPower.toIntOrNull() ?: 17
                        val s = spreadingFactor.toIntOrNull() ?: 8
                        val c = codingRate.toIntOrNull() ?: 6
                        rnsService.injectRNode(f, b, t, s, c) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = rnsStatus.isConnected,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                ) {
                    Text("Apply Tuning", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // LXMF Sync Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.padding(8.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("LXMF Sync", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text("Off-grid Data Exchange", style = MaterialTheme.typography.labelMedium, color = Gray500)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Destination Hash (Hex)", style = MaterialTheme.typography.labelLarge, color = Gray500)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("e.g. 8b4f2c...", style = MaterialTheme.typography.labelLarge, color = Gray300) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Gray400, modifier = Modifier.size(20.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary600,
                            unfocusedBorderColor = Gray100
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { rnsService.announce() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = rnsStatus.isRnsRunning,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Gray100)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Announce", style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = { 
                            isSyncing = true
                            rnsService.sendText("8b4f2c...", "SYNC_RECORDS_MOCK")
                        },
                        modifier = Modifier.weight(2f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = rnsStatus.isRnsRunning && !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary600)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync 12 Records", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    if (showDevicePicker) {
        AlertDialog(
            onDismissRequest = { showDevicePicker = false },
            title = { Text("Select RNode Device") },
            text = {
                Column {
                    if (pairedDevices.isEmpty()) {
                        Text("No paired devices found. Pair your RNode in Bluetooth settings first.")
                    } else {
                        pairedDevices.forEach { (name, address) ->
                            TextButton(
                                onClick = {
                                    rnsService.connectRNode(address)
                                    showDevicePicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(name, style = MaterialTheme.typography.bodyLarge)
                                    Text(address, style = MaterialTheme.typography.labelSmall, color = Gray400)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDevicePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
