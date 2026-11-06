package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.logging.AppLogger
import com.example.core.model.LogEntry
import com.example.core.model.LogLevel
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanBright
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.RoseNeon
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogMonitorScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val logs by AppLogger.logs.collectAsState()

    var levelFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var viewingLogDetail by remember { mutableStateOf<LogEntry?>(null) }

    // Export logs launcher
    val exportLogsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(AppLogger.exportAsText().toByteArray(Charsets.UTF_8))
                Toast.makeText(context, "Log berhasil diekspor!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate900)
                .border(1.dp, Slate800)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldBright.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(22.dp))
                    }

                    Column {
                        Text(
                            text = "LIVE ERROR & ACTIVITY MONITOR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = EmeraldBright,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${logs.size} log peristiwa tercatat",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(AppLogger.exportAsText()))
                            Toast.makeText(context, "Log disalin ke clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanBright, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { exportLogsLauncher.launch("apk_inspector_logs_${System.currentTimeMillis()}.txt") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.DriveFileMove, contentDescription = "Export", tint = EmeraldBright, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { AppLogger.clear() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = RoseNeon, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter teks pesan atau tag...", color = Slate400, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = Slate700,
                    focusedContainerColor = Slate850,
                    unfocusedContainerColor = Slate850,
                    focusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Level Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val levels = listOf("ALL", "DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL")
                levels.forEach { lvl ->
                    val selected = levelFilter == lvl
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) EmeraldBright else Slate850)
                            .border(1.dp, if (selected) EmeraldBright else Slate700, RoundedCornerShape(6.dp))
                            .clickable { levelFilter = lvl }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = lvl,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Slate950 else Slate200
                        )
                    }
                }
            }
        }

        // Filtered logs list
        val filteredLogs = remember(logs, levelFilter, searchQuery) {
            logs.filter { entry ->
                val matchesLevel = levelFilter == "ALL" || entry.level.name == levelFilter
                val matchesSearch = searchQuery.isBlank() ||
                        entry.message.contains(searchQuery, ignoreCase = true) ||
                        entry.tag.contains(searchQuery, ignoreCase = true)
                matchesLevel && matchesSearch
            }
        }

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada log aktivitas yang cocok.", color = Slate400, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredLogs, key = { it.id }) { entry ->
                    LogEntryCard(
                        entry = entry,
                        onClick = { viewingLogDetail = entry }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Stack Trace / Detail Dialog
    if (viewingLogDetail != null) {
        val entry = viewingLogDetail!!
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        AlertDialog(
            onDismissRequest = { viewingLogDetail = null },
            containerColor = Slate900,
            title = {
                Text(
                    text = "[${entry.level}] ${entry.tag}",
                    color = getLogLevelColor(entry.level),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(modifier = Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Waktu: ${sdf.format(Date(entry.timestamp))}", fontSize = 11.sp, color = Slate400)
                    if (entry.durationMs != null) {
                        Text(text = "Durasi: ${entry.durationMs} ms", fontSize = 11.sp, color = CyanBright)
                    }
                    Text(text = "Pesan:\n${entry.message}", fontSize = 12.sp, color = Color.White)

                    if (!entry.errorStack.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Stack Trace:", fontSize = 11.sp, color = RoseNeon, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Slate950)
                                .border(1.dp, Slate800, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            LazyColumn {
                                item {
                                    Text(
                                        text = entry.errorStack,
                                        fontSize = 10.sp,
                                        color = Slate200,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingLogDetail = null }) {
                    Text("Tutup", color = CyanBright)
                }
            }
        )
    }
}

@Composable
fun LogEntryCard(
    entry: LogEntry,
    onClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    val levelColor = getLogLevelColor(entry.level)

    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate800, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(levelColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = entry.level.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = levelColor)
                    }

                    Text(text = entry.tag, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Text(text = sdf.format(Date(entry.timestamp)), fontSize = 10.sp, color = Slate400, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.message,
                fontSize = 12.sp,
                color = Slate200,
                maxLines = 2
            )

            if (entry.errorStack != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "▶ Klik untuk melihat Stack Trace", fontSize = 10.sp, color = RoseNeon, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

fun getLogLevelColor(level: LogLevel): Color {
    return when (level) {
        LogLevel.DEBUG -> Slate400
        LogLevel.INFO -> CyanBright
        LogLevel.WARNING -> AmberNeon
        LogLevel.ERROR -> RoseNeon
        LogLevel.CRITICAL -> PurpleNeon
    }
}
