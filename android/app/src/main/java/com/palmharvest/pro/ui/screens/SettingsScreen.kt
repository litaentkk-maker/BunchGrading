package com.palmharvest.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palmharvest.pro.ui.theme.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onOpenRNS: () -> Unit = {},
    onExportCSV: () -> Unit = {},
    onExportSheets: () -> Unit = {},
    onClearData: () -> Unit = {}
) {
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
                    text = "Settings",
                    style = MaterialTheme.typography.displaySmall,
                    color = Gray900
                )
                Text(
                    text = "Manage your application",
                    style = MaterialTheme.typography.labelMedium,
                    color = Gray500
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Off-Grid Sync Card
        SettingsCard(
            title = "Off-Grid Sync",
            description = "Reticulum Network Stack (RNS)",
            icon = Icons.Default.Refresh,
            iconColor = Color(0xFFEA580C),
            iconBg = Color(0xFFFFF7ED)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF7ED).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("RNS Bridge", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(
                            "Sync harvest data via LoRa when cellular network is unavailable.",
                            style = MaterialTheme.typography.labelMedium,
                            color = Gray500,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Button(
                        onClick = onOpenRNS,
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                    ) {
                        Text("Open Bridge", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Export Data Card
        SettingsCard(
            title = "Export Data",
            description = "Download your harvest history",
            icon = Icons.Default.CloudUpload,
            iconColor = Color(0xFF16A34A),
            iconBg = Color(0xFFF0FDF4)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ExportButton(
                    label = "Export CSV",
                    icon = Icons.Default.Storage,
                    iconColor = Gray600,
                    iconBg = Gray100,
                    onClick = onExportCSV,
                    modifier = Modifier.weight(1f)
                )
                ExportButton(
                    label = "Google Sheets",
                    icon = Icons.Default.Cloud,
                    iconColor = Color(0xFF16A34A),
                    iconBg = Color(0xFFDCFCE7),
                    onClick = onExportSheets,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "CSV exports are immediate. Google Sheets sync requires an active cloud connection.",
                style = MaterialTheme.typography.labelMedium,
                color = Gray400,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Data Management Card
        SettingsCard(
            title = "Data Management",
            description = "Local storage and exports",
            icon = Icons.Default.Storage,
            iconColor = Color(0xFF9333EA),
            iconBg = Color(0xFFFAF5FF)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Local Records", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("Currently stored on this device", style = MaterialTheme.typography.labelMedium, color = Gray500)
                    }
                    Text("Active", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onClearData,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "Clear Local Cache",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "PalmHarvest Pro v1.0.2",
            style = MaterialTheme.typography.labelMedium,
            color = Gray400,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun SettingsCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    content: @Composable () -> Unit
) {
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
                    color = iconBg
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(8.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(description, style = MaterialTheme.typography.labelMedium, color = Gray500)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            content()
        }
    }
}

@Composable
fun ExportButton(
    label: String,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Gray100),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gray900)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = iconBg
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        }
    }
}
