package com.palmharvest.pro.ui.screens

import android.content.Context
import com.palmharvest.pro.RNSService
import com.palmharvest.pro.StorageManager
import com.palmharvest.pro.HarvestRecord
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import java.util.UUID
import java.util.Calendar
import android.location.Location
import android.location.LocationManager
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    user: String,
    onLogout: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("capture") }
    var capturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var editingRecord by remember { mutableStateOf<HarvestRecord?>(null) }
    var currentLat by remember { mutableStateOf<Double?>(null) }
    var currentLng by remember { mutableStateOf<Double?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val rnsService = remember { RNSService(context) }
    val storageManager = remember { StorageManager(context) }
    
    var records by remember { mutableStateOf(storageManager.getRecords()) }

    LaunchedEffect(user) {
        rnsService.startRNS(user)
    }

    fun getLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) 
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            currentLat = location?.latitude
            currentLng = location?.longitude
        }
    }

    fun saveBitmapAsWebP(bitmap: Bitmap): String {
        val filename = "harvest_${System.currentTimeMillis()}.webp"
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { out ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 50, out)
            } else {
                @Suppress("DEPRECATION")
                bitmap.compress(Bitmap.CompressFormat.WEBP, 50, out)
            }
        }
        return file.absolutePath
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            Header(
                user = user,
                onLogout = onLogout,
                onSettings = { currentScreen = "settings" },
                isOnline = true,
                lastSync = "10:23 AM"
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        },
        containerColor = Gray50
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                "capture" -> CaptureScreen(
                    recentRecords = records.take(5),
                    onCapture = { 
                        capturedBitmap = it
                        editingRecord = null
                        getLocation()
                        currentScreen = "entry" 
                    },
                    onOpenRNS = { currentScreen = "rns" },
                    onEditRecord = { record ->
                        editingRecord = record
                        currentScreen = "entry"
                    }
                )
                "calendar" -> CalendarScreen(
                    records = records,
                    onEditRecord = { record ->
                        editingRecord = record
                        currentScreen = "entry"
                    }
                )
                "settings" -> SettingsScreen(
                    onBack = { currentScreen = "capture" },
                    onOpenRNS = { currentScreen = "rns" },
                    onClearData = {
                        storageManager.clearAll()
                        records = emptyList()
                    }
                )
                "rns" -> RNSScreen(
                    rnsService = rnsService,
                    onBack = { currentScreen = "settings" }
                )
                "entry" -> EntryScreen(
                    photo = capturedBitmap,
                    initialBunchCount = editingRecord?.bunchCount ?: 1,
                    initialBlockId = editingRecord?.blockId ?: storageManager.getLastBlockId(),
                    latitude = editingRecord?.latitude ?: currentLat,
                    longitude = editingRecord?.longitude ?: currentLng,
                    onSave = { bunchCount, blockId -> 
                        if (editingRecord != null) {
                            val updatedRecord = editingRecord!!.copy(
                                bunchCount = bunchCount,
                                blockId = blockId
                            )
                            storageManager.saveRecord(updatedRecord)
                        } else {
                            val todayStart = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            
                            val todaysRecords = records.filter { 
                                it.harvesterUid == user && it.timestamp >= todayStart 
                            }
                            val maxSeq = todaysRecords.mapNotNull { 
                                it.collectionPoint.substringAfterLast("-").toIntOrNull() 
                            }.maxOrNull() ?: 0
                            val nextSeq = maxSeq + 1
                            
                            val name = user.split("@").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "User"
                            val collectionPoint = String.format("%s-%02d", name, nextSeq)

                            val photoPath = capturedBitmap?.let { saveBitmapAsWebP(it) } ?: ""

                            val newRecord = HarvestRecord(
                                id = UUID.randomUUID().toString(),
                                harvesterUid = user,
                                harvesterName = name,
                                blockId = blockId,
                                collectionPoint = collectionPoint,
                                bunchCount = bunchCount,
                                photoUrl = photoPath,
                                timestamp = System.currentTimeMillis(),
                                latitude = currentLat,
                                longitude = currentLng
                            )
                            storageManager.saveRecord(newRecord)
                            storageManager.setLastBlockId(blockId)
                        }
                        records = storageManager.getRecords()
                        editingRecord = null
                        currentScreen = "capture" 
                    },
                    onCancel = { 
                        editingRecord = null
                        currentScreen = "capture" 
                    }
                )
            }
        }
    }
}

@Composable
fun Header(
    user: String,
    onLogout: () -> Unit,
    onSettings: () -> Unit,
    isOnline: Boolean,
    lastSync: String? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Primary600
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "PalmHarvest Pro",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Gray900
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOnline) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                        if (lastSync != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sync: $lastSync",
                                style = MaterialTheme.typography.labelMedium,
                                color = Gray400
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Gray500)
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Gray500)
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentScreen: String,
    onScreenSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = White,
        tonalElevation = 8.dp,
        modifier = Modifier.height(80.dp)
    ) {
        BottomNavItem(
            label = "Capture",
            icon = Icons.Default.CameraAlt,
            selected = currentScreen == "capture",
            onClick = { onScreenSelected("capture") }
        )
        BottomNavItem(
            label = "Calendar",
            icon = Icons.Default.CalendarToday,
            selected = currentScreen == "calendar",
            onClick = { onScreenSelected("calendar") }
        )
        BottomNavItem(
            label = "Settings",
            icon = Icons.Default.Settings,
            selected = currentScreen == "settings",
            onClick = { onScreenSelected("settings") }
        )
    }
}

@Composable
fun RowScope.BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        },
        label = {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Primary600,
            selectedTextColor = Primary600,
            unselectedIconColor = Gray400,
            unselectedTextColor = Gray400,
            indicatorColor = Primary50
        )
    )
}

// Placeholder Screens removed, using real screens from package
