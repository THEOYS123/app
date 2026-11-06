package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
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

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var autoDecodeXml by remember { mutableStateOf(true) }
    var searchMaxFileSizeMb by remember { mutableStateOf("2 MB") }
    var hexChunkLines by remember { mutableStateOf("300 baris") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "PENGATURAN DEEP APK INSPECTOR",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = CyanBright,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Konfigurasi mesin pemindai, batas memori, dan preferensi kerja",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400
            )
        }

        // Section: Parser & Indexing
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Slate800, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = CyanBright, modifier = Modifier.size(20.dp))
                        Text("DEKOMPILASI & PENCARIAN", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyanBright)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dekompresi Binary XML Otomatis", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Mendekompilasi AndroidManifest.xml dan file res/*.xml secara real-time ke teks XML terbaca", color = Slate400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = autoDecodeXml,
                            onCheckedChange = { autoDecodeXml = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Slate950, checkedTrackColor = CyanBright)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Batas Maksimal Content Search", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Membatasi pembacaan file besar agar UI tidak mengalami freeze", color = Slate400, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Slate800)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(searchMaxFileSizeMb, fontSize = 12.sp, color = CyanBright, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ukuran Chunk Hex Viewer", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Jumlah baris biner per halaman render", color = Slate400, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Slate800)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(hexChunkLines, fontSize = 12.sp, color = CyanBright, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: Storage & Cache Cleaning
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Slate800, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = AmberNeon, modifier = Modifier.size(20.dp))
                        Text("PEMBERSIHAN CACHE & PENYIMPANAN", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmberNeon)
                    }

                    Text(
                        text = "Membersihkan file APK sementara yang disalin saat membuka APK melalui Storage Access Framework (SAF).",
                        fontSize = 11.sp,
                        color = Slate400
                    )

                    Button(
                        onClick = {
                            val cacheDir = context.cacheDir
                            val files = cacheDir.listFiles { _, name -> name.startsWith("inspect_target_") }
                            var deletedCount = 0
                            files?.forEach {
                                if (it.delete()) deletedCount++
                            }
                            Toast.makeText(context, "Berhasil membersihkan $deletedCount file cache APK", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("BERSIHKAN CACHE INSPEKSI APK", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Security & Disclaimer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Slate800, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(20.dp))
                        Text("INFORMASI LEGAL & KEAMANAN", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldBright)
                    }

                    Text(
                        text = "• DEEP APK INSPECTOR adalah aplikasi reverse engineering, audit keamanan, dan file explorer untuk pengembang Android.\n" +
                                "• Semua proses pemeriksaan dan ekstraksi dilakukan 100% secara lokal pada perangkat pengguna.\n" +
                                "• Tidak ada kode rahasia atau data pengguna yang diunggah ke server.\n" +
                                "• Versi Aplikasi: 1.0.0 (Native Kotlin / Jetpack Compose / M3)",
                        fontSize = 11.sp,
                        color = Slate200,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
