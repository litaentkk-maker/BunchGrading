package com.palmharvest.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.palmharvest.pro.ui.theme.*
import com.palmharvest.pro.HarvestRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    records: List<HarvestRecord> = emptyList(),
    onExportCSV: () -> Unit = {},
    onExportSheets: () -> Unit = {}
) {
    val todayCal = Calendar.getInstance()
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    var selectedYear by remember { mutableStateOf(todayYear) }
    var selectedMonth by remember { mutableStateOf(todayMonth) }
    var selectedDay by remember { mutableStateOf(todayDay) }

    var displayYear by remember { mutableStateOf(todayYear) }
    var displayMonth by remember { mutableStateOf(todayMonth) }

    val displayCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, displayYear)
        set(Calendar.MONTH, displayMonth)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = displayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = displayCal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val displayMonthName = monthFormat.format(displayCal.time)

    // Filter records for selected date
    val selectedDateStart = Calendar.getInstance().apply {
        set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val selectedDateEnd = selectedDateStart + 86400000L

    val dailyRecords = records.filter { it.timestamp in selectedDateStart until selectedDateEnd }
    val dailyBunches = dailyRecords.sumOf { it.bunchCount }
    val dailyPoints = dailyRecords.map { it.collectionPoint }.distinct().size

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

        // Calendar Card
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
                    Text(displayMonthName, style = MaterialTheme.typography.headlineMedium, color = Gray900)
                    Row {
                        IconButton(onClick = {
                            if (displayMonth == 0) {
                                displayMonth = 11
                                displayYear--
                            } else {
                                displayMonth--
                            }
                        }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month") }
                        IconButton(onClick = {
                            if (displayMonth == 11) {
                                displayMonth = 0
                                displayYear++
                            } else {
                                displayMonth++
                            }
                        }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month") }
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
                    
                    val totalCells = firstDayOfWeek + daysInMonth
                    val rows = Math.ceil(totalCells / 7.0).toInt()
                    var currentDay = 1
                    
                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                if (cellIndex < firstDayOfWeek || currentDay > daysInMonth) {
                                    Spacer(modifier = Modifier.size(40.dp))
                                } else {
                                    val day = currentDay
                                    val isSelected = day == selectedDay && displayMonth == selectedMonth && displayYear == selectedYear
                                    val isToday = day == todayDay && displayMonth == todayMonth && displayYear == todayYear
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isSelected -> Primary600
                                                    isToday -> Primary50
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                selectedDay = day
                                                selectedMonth = displayMonth
                                                selectedYear = displayYear
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$day",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = when {
                                                isSelected -> White
                                                isToday -> Primary700
                                                else -> Gray900
                                            },
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    currentDay++
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Daily Summary & Entries
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                val selectedDateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateStart))
                Text(
                    text = "Summary for $selectedDateStr",
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
                        Text("$dailyBunches", style = MaterialTheme.typography.displaySmall, color = Primary700)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Collection Points", style = MaterialTheme.typography.labelMedium, color = Gray400)
                        Text("$dailyPoints", style = MaterialTheme.typography.displaySmall, color = Gray900)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Gray100)
                Spacer(modifier = Modifier.height(16.dp))

                if (dailyRecords.isEmpty()) {
                    Text(
                        text = "No harvests recorded on this date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray500,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    dailyRecords.forEachIndexed { index, record ->
                        HarvestItem(
                            point = record.collectionPoint,
                            bunches = record.bunchCount,
                            time = timeFormat.format(Date(record.timestamp))
                        )
                        if (index < dailyRecords.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Gray100)
                        }
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
    }
}
