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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palmharvest.pro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    photo: android.graphics.Bitmap? = null,
    initialBunchCount: Int = 1,
    onSave: (Int) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    var bunchCount by remember(initialBunchCount) { mutableStateOf(initialBunchCount) }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray50)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancel,
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
                    text = "Bunch Data",
                    style = MaterialTheme.typography.displaySmall,
                    color = Gray900
                )
                Text(
                    text = "Input harvest details",
                    style = MaterialTheme.typography.labelMedium,
                    color = Gray500
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Photo Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Primary600)
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = White.copy(alpha = 0.2f)
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = null, tint = White, modifier = Modifier.padding(12.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Bunches Photo", style = MaterialTheme.typography.titleLarge, color = White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Primary100, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lat: 3.1415, Lng: 101.6869", style = MaterialTheme.typography.labelMedium, color = Primary100)
                            }
                        }
                    }
                }
                
                // Photo Placeholder or Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(Gray100),
                    contentAlignment = Alignment.Center
                ) {
                    if (photo != null) {
                        androidx.compose.foundation.Image(
                            bitmap = photo.asImageBitmap(),
                            contentDescription = "Captured Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = Gray300)
                    }
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .background(Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Evidence Captured", style = MaterialTheme.typography.labelMedium, color = White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Input Card
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
                        Icon(Icons.Default.Inventory, contentDescription = null, tint = Primary600, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Number of Bunches", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    }
                    Surface(
                        color = Primary50,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "$bunchCount",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.titleLarge,
                            color = Primary700,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Slider(
                    value = bunchCount.toFloat(),
                    onValueChange = { bunchCount = it.toInt() },
                    valueRange = 1f..200f,
                    steps = 199,
                    colors = SliderDefaults.colors(
                        thumbColor = Primary600,
                        activeTrackColor = Primary600,
                        inactiveTrackColor = Gray100
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manual Input", style = MaterialTheme.typography.labelMedium, color = Gray400)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = bunchCount.toString(),
                            onValueChange = { 
                                val valInt = it.toIntOrNull()
                                if (valInt != null) bunchCount = valInt.coerceIn(1, 200)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary600,
                                unfocusedBorderColor = Gray200
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(modifier = Modifier.padding(bottom = 4.dp)) {
                        listOf(5, 10, 20).forEach { valToAdd ->
                            OutlinedButton(
                                onClick = { bunchCount = (bunchCount + valToAdd).coerceAtMost(200) },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary600)
                            ) {
                                Text("+$valToAdd", style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onSave(bunchCount) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary600),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Save Harvest", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
