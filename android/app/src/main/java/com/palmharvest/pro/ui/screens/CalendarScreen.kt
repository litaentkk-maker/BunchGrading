package com.palmharvest.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palmharvest.pro.ui.theme.*
import com.palmharvest.pro.HarvestRecord

@Composable
fun CalendarScreen(
    records: List<HarvestRecord> = emptyList(),
    onExportCSV: () -> Unit = {},
    onExportSheets: () -> Unit = {}
) {
    val totalBunches = records.sumOf { it.bunchCount }
    val uniquePoints = records.map { it.collectionPoint }.distinct().size

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
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Harvest Calendar",
            style = MaterialTheme.typography.displaySmall,
            color = Gray900
        )
        Text(
            text = "Track your performance and export data",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray500,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Calendar Placeholder
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
                    Text("April 2026", style = MaterialTheme.typography.headlineMedium, color = Gray900)
                    Row {
                        IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) }
                        IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Calendar Grid
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.width(40.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Gray400,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val days = (1..30).toList()
                    val rows = days.chunked(7)
                    
                    rows.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            row.forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (day == 5) Primary600 else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$day",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (day == 5) White else Gray900,
                                        fontWeight = if (day == 5) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                            // Fill empty slots in the last row
                            if (row.size < 7) {
                                repeat(7 - row.size) {
                                    Spacer(modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Export Actions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onExportCSV,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gray900)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export CSV", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onExportSheets,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary600)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Google Sheets", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Daily Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Daily Summary",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Gray900
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Bunches", style = MaterialTheme.typography.labelMedium, color = Gray400)
                        Text("$totalBunches", style = MaterialTheme.typography.displaySmall, color = Primary700)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Collection Points", style = MaterialTheme.typography.labelMedium, color = Gray400)
                        Text("$uniquePoints", style = MaterialTheme.typography.displaySmall, color = Gray900)
                    }
                }
            }
        }
    }
}
