package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AppInfo
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanBright
import com.example.ui.theme.EmeraldBright
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
fun AppSelectorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val installedApps by viewModel.installedApps.collectAsState()
    val filterType by viewModel.appFilterType.collectAsState()
    val searchQuery by viewModel.appSearchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // SAF Picker Launcher for APK files
    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.inspectApkUri(uri, uri.lastPathSegment ?: "External APK")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Slate900,
            contentColor = CyanBright,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyanBright
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Aplikasi Terinstal (${installedApps.size})",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_installed_apps")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Buka File APK (SAF)",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_saf_apk")
            )
        }

        if (selectedTab == 0) {
            // Tab 1: Installed Apps
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar & Refresh
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setAppSearch(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_installed_apps_input"),
                        placeholder = { Text("Cari nama aplikasi atau package...", color = Slate400, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanBright) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanBright,
                            unfocusedBorderColor = Slate700,
                            focusedContainerColor = Slate900,
                            unfocusedContainerColor = Slate900,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Slate200
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    IconButton(
                        onClick = { viewModel.refreshInstalledApps() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate900)
                            .border(1.dp, Slate700, RoundedCornerShape(12.dp))
                            .testTag("btn_refresh_installed_apps")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanBright)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        label = "Semua (${installedApps.size})",
                        selected = filterType == "ALL",
                        onClick = { viewModel.setAppFilter("ALL") }
                    )
                    val userAppsCount = installedApps.count { !it.isSystemApp }
                    FilterChip(
                        label = "User Apps ($userAppsCount)",
                        selected = filterType == "USER",
                        onClick = { viewModel.setAppFilter("USER") }
                    )
                    val systemAppsCount = installedApps.count { it.isSystemApp }
                    FilterChip(
                        label = "System ($systemAppsCount)",
                        selected = filterType == "SYSTEM",
                        onClick = { viewModel.setAppFilter("SYSTEM") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filtered apps list
                val filteredApps = remember(installedApps, filterType, searchQuery) {
                    installedApps.filter { app ->
                        val matchesFilter = when (filterType) {
                            "USER" -> !app.isSystemApp
                            "SYSTEM" -> app.isSystemApp
                            else -> true
                        }
                        val matchesQuery = searchQuery.isBlank() ||
                                app.appName.contains(searchQuery, ignoreCase = true) ||
                                app.packageName.contains(searchQuery, ignoreCase = true)
                        matchesFilter && matchesQuery
                    }
                }

                if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = Slate600, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Tidak ada aplikasi cocok dengan '$searchQuery'" else "Memuat aplikasi...",
                                color = Slate400,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            AppInfoCard(
                                app = app,
                                onInspect = { viewModel.inspectInstalledApp(app) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        } else {
            // Tab 2: SAF APK File Chooser
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyanBright.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CyanBright.copy(alpha = 0.15f))
                                .border(1.dp, CyanBright.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = CyanBright,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Storage Access Framework (SAF)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pilih file .apk dari folder Downloads, SD Card, atau penyimpanan cloud menggunakan Document Picker resmi Android.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                apkPickerLauncher.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanBright),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_pick_apk_saf")
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, tint = Slate950)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PILIH FILE APK", color = Slate950, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Security & Privacy Explanation Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate800, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "KEBIJAKAN AKSES DOKUMEN & PRIVASI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberNeon,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Storage Access Framework adalah mekanisme resmi Google untuk mengakses file tanpa memerlukan izin berbahaya yang luas.\n" +
                                    "• Hanya file APK yang secara eksplisit dipilih oleh pengguna yang akan dibaca dan disalin ke cache inspeksi lokal.\n" +
                                    "• Aplikasi ini tidak mengakses direktori privat atau data login aplikasi lain.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate200,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) CyanBright else Slate900)
            .border(1.dp, if (selected) CyanBright else Slate700, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Slate950 else Slate200
        )
    }
}

@Composable
fun AppInfoCard(
    app: AppInfo,
    onInspect: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate800, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (app.isSystemApp) AmberNeon.copy(alpha = 0.15f) else EmeraldBright.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = if (app.isSystemApp) AmberNeon else EmeraldBright,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = app.appName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (app.isSystemApp) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AmberNeon.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("SYSTEM", fontSize = 9.sp, color = AmberNeon, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = app.packageName,
                        fontSize = 12.sp,
                        color = Slate400,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "v${app.versionName} (${app.versionCode})",
                    fontSize = 11.sp,
                    color = Slate400
                )
                Text(text = "•", fontSize = 11.sp, color = Slate600)
                Text(
                    text = "${app.sizeBytes / (1024 * 1024)} MB",
                    fontSize = 11.sp,
                    color = CyanBright,
                    fontWeight = FontWeight.SemiBold
                )
                if (app.installTime > 0) {
                    Text(text = "•", fontSize = 11.sp, color = Slate600)
                    Text(
                        text = "Instal: ${sdf.format(Date(app.installTime))}",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // APK Path snippet
            Text(
                text = app.apkPath,
                fontSize = 10.sp,
                color = Slate600,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Button
            Button(
                onClick = onInspect,
                colors = ButtonDefaults.buttonColors(containerColor = CyanBright),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("btn_inspect_${app.packageName}")
            ) {
                Text("INSPEKSI STRUKTUR APK", color = Slate950, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
