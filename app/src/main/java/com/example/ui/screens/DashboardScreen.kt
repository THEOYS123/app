package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.BlueNeon
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.RoseNeon
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentArchive by viewModel.currentArchive.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val totalAnalyzed by viewModel.totalApksAnalyzedCount.collectAsState()
    val workspaceItems by viewModel.workspaceItems.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Hero Title Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Slate900, Slate850, Slate800)
                        )
                    )
                    .border(1.dp, CyanBright.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyanBright.copy(alpha = 0.15f))
                                .border(1.dp, CyanBright.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security Shield",
                                tint = CyanBright,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "DEEP APK INSPECTOR",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = CyanBright,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Advanced Application Reverse Engineering & Explorer",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Analisis mendalam struktur internal APK, inspeksi DEX bytecode, dekompilasi binary XML Android, pencarian teks tingkat lanjut, dan modifikasi workspace lokal yang aman.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate200,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Active Inspection Target Card (if any)
        item {
            if (currentArchive != null) {
                val archive = currentArchive!!
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EmeraldBright.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .testTag("active_archive_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldNeon)
                                )
                                Text(
                                    text = "TARGET AKTIF",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = EmeraldBright,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "${archive.totalEntries} entri",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = archive.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = archive.sourcePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatChip(label = "Ukuran", value = "${archive.totalUncompressedBytes / (1024 * 1024)} MB", color = CyanBright)
                            StatChip(label = "DEX", value = "${archive.dexCount} file", color = PurpleNeon)
                            StatChip(label = "Native ABI", value = if (archive.nativeAbis.isNotEmpty()) archive.nativeAbis.joinToString() else "None", color = AmberNeon)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyanBright)
                                    .clickable { onNavigate(AppScreen.EXPLORER) }
                                    .padding(vertical = 10.dp)
                                    .testTag("btn_goto_explorer"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Jelajahi File", color = Slate900, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate800)
                                    .clickable { onNavigate(AppScreen.SEARCH) }
                                    .padding(vertical = 10.dp)
                                    .testTag("btn_goto_search"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Pencarian", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate800)
                                    .clickable { onNavigate(AppScreen.DEX_ANALYZER) }
                                    .padding(vertical = 10.dp)
                                    .testTag("btn_goto_dex"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("DEX Info", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Statistics Metric Cards
        item {
            Text(
                text = "STATISTIK PEMINDAIAN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Aplikasi Terinstal",
                    value = "${installedApps.size}",
                    sub = "Dapat diakses",
                    icon = Icons.Default.Apps,
                    accentColor = CyanBright,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Dianalisis",
                    value = "$totalAnalyzed",
                    sub = "Sesi ini",
                    icon = Icons.Default.Storage,
                    accentColor = EmeraldBright,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "File di Workspace",
                    value = "${workspaceItems.size}",
                    sub = "Terekstrak/diedit",
                    icon = Icons.Default.Workspaces,
                    accentColor = AmberNeon,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "File Index Aktif",
                    value = "${currentArchive?.totalEntries ?: 0}",
                    sub = "Dalam arsip",
                    icon = Icons.Default.Description,
                    accentColor = BlueNeon,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Actions Grid
        item {
            Text(
                text = "NAVIGASI & ALAT UTAMA",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionRow(
                    title = "Pilih Aplikasi / Buka File APK",
                    subtitle = "Jelajahi aplikasi terinstal atau buka APK melalui SAF",
                    icon = Icons.Default.FolderOpen,
                    accentColor = CyanBright,
                    onClick = { onNavigate(AppScreen.APP_SELECTOR) },
                    tag = "nav_apps"
                )
                QuickActionRow(
                    title = "Deep File Explorer",
                    subtitle = "Hierarki direktori APK, Manifest, Assets, Res, DEX",
                    icon = Icons.Default.Storage,
                    accentColor = EmeraldBright,
                    onClick = { onNavigate(AppScreen.EXPLORER) },
                    tag = "nav_explorer"
                )
                QuickActionRow(
                    title = "Mesin Pencarian Mendalam",
                    subtitle = "Pencarian nama, regex, ekstensi, dan isi file (content search)",
                    icon = Icons.Default.Search,
                    accentColor = BlueNeon,
                    onClick = { onNavigate(AppScreen.SEARCH) },
                    tag = "nav_search"
                )
                QuickActionRow(
                    title = "DEX & Native Analyzer",
                    subtitle = "Inspeksi header bytecode DEX, daftar class, method & library .so",
                    icon = Icons.Default.Description,
                    accentColor = PurpleNeon,
                    onClick = { onNavigate(AppScreen.DEX_ANALYZER) },
                    tag = "nav_dex"
                )
                QuickActionRow(
                    title = "Modification Workspace",
                    subtitle = "Kelola file terekstrak, bandingkan diff, ekspor ZIP/APK",
                    icon = Icons.Default.Workspaces,
                    accentColor = AmberNeon,
                    onClick = { onNavigate(AppScreen.WORKSPACE) },
                    tag = "nav_workspace"
                )
                QuickActionRow(
                    title = "Android Permission Center",
                    subtitle = "Pemeriksaan izin, transparansi batasan sandbox Android",
                    icon = Icons.Default.Lock,
                    accentColor = RoseNeon,
                    onClick = { onNavigate(AppScreen.PERMISSIONS) },
                    tag = "nav_permissions"
                )
                QuickActionRow(
                    title = "Live Error & Activity Monitor",
                    subtitle = "Pantau log sistem, stack trace error, benchmark durasi",
                    icon = Icons.Default.BugReport,
                    accentColor = EmeraldNeon,
                    onClick = { onNavigate(AppScreen.LOGS) },
                    tag = "nav_logs"
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    sub: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, Slate800, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.Medium)
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = sub, fontSize = 11.sp, color = Slate400)
        }
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Slate800)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$label:", fontSize = 10.sp, color = Slate400)
            Text(text = value, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuickActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Slate900)
            .border(1.dp, Slate800, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
            Text(text = subtitle, fontSize = 11.sp, color = Slate400)
        }
    }
}
