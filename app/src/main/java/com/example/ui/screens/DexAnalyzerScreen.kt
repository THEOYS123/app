package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.archive.ApkArchiveManager
import com.example.core.model.DexAnalysisResult
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.BlueNeon
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
fun DexAnalyzerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentArchive by viewModel.currentArchive.collectAsState()
    val dexResults by viewModel.dexResults.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var classSearchQuery by remember { mutableStateOf("") }

    if (currentArchive == null) {
        Box(modifier = modifier.fillMaxSize().background(Slate950), contentAlignment = Alignment.Center) {
            Text("Pilih aplikasi atau file APK terlebih dahulu untuk menganalisis DEX.", color = Slate400)
        }
        return
    }

    // Native libs entries
    val nativeLibEntries = remember(currentArchive) {
        ApkArchiveManager.getAllEntries().filter { it.path.startsWith("lib/") && it.extension == "so" }
    }

    // Detect libraries
    val detectedLibraries = remember(dexResults, nativeLibEntries) {
        detectThirdPartyLibraries(dexResults, nativeLibEntries.map { it.name })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate900)
                .border(1.dp, Slate800)
                .padding(16.dp)
        ) {
            Text(
                text = "DEX & NATIVE ANALYZER",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = PurpleNeon,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${currentArchive!!.name} • ${dexResults.size} DEX file(s) • ${nativeLibEntries.size} Shared Libraries (.so)",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400
            )
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Slate900,
            contentColor = PurpleNeon,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = PurpleNeon
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Header DEX", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Daftar Class", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Native Lib (.so)", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Framework / SDK", fontSize = 12.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        when (selectedTab) {
            0 -> {
                // Header DEX Breakdown
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(dexResults) { dex ->
                        DexHeaderCard(dex = dex)
                    }
                }
            }
            1 -> {
                // Class List
                val allClasses = remember(dexResults) { dexResults.flatMap { it.sampleClasses }.distinct() }
                val filteredClasses = remember(allClasses, classSearchQuery) {
                    if (classSearchQuery.isBlank()) allClasses
                    else allClasses.filter { it.contains(classSearchQuery, ignoreCase = true) }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = classSearchQuery,
                        onValueChange = { classSearchQuery = it },
                        placeholder = { Text("Cari class atau package...", color = Slate400, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleNeon,
                            unfocusedBorderColor = Slate700,
                            focusedContainerColor = Slate900,
                            unfocusedContainerColor = Slate900,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Menampilkan ${filteredClasses.size} class yang terdaftar",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredClasses) { className ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate900)
                                    .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Code, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = className,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Slate200
                                    )
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Native Libraries (.so)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, RoseNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("ARSITEKTUR NATIVE (ABI)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoseNeon)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (currentArchive!!.nativeAbis.isNotEmpty()) currentArchive!!.nativeAbis.joinToString(" • ") else "Tidak memuat native libraries (Murni Java/Kotlin Bytecode)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    items(nativeLibEntries) { lib ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, Slate800, RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(RoseNeon.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = RoseNeon, modifier = Modifier.size(20.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = lib.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text(text = "${lib.path} (${lib.sizeBytes / 1024} KB)", fontSize = 11.sp, color = Slate400, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // Frameworks / Third-Party SDK Detection
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "IDENTIFIKASI KOMPONEN & FRAMEWORK EKSTERNAL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Berdasarkan analisis pattern class, package, dan shared library yang ditemukan di dalam arsip APK:",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }

                    items(detectedLibraries) { lib ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, Slate800, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(EmeraldBright.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(22.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = lib.first, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text(text = lib.second, fontSize = 11.sp, color = Slate400)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DexHeaderCard(dex: DexAnalysisResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, PurpleNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = dex.dexFileName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PurpleNeon)
                Text(text = "${dex.fileSize / 1024} KB", fontSize = 12.sp, color = Slate400)
            }

            DetailRow("Magic", dex.magic)
            DetailRow("Adler32 Checksum", "0x%08X".format(dex.checksum))
            DetailRow("SHA-1 Signature", dex.signatureHex)
            DetailRow("Header Size", "${dex.headerSize} byte")
            DetailRow("String IDs", "${dex.stringIdsSize} string")
            DetailRow("Type IDs", "${dex.typeIdsSize} tipe")
            DetailRow("Field IDs", "${dex.fieldIdsSize} field")
            DetailRow("Method IDs", "${dex.methodIdsSize} method")
            DetailRow("Class Definitions", "${dex.classDefsSize} class def")
        }
    }
}

private fun detectThirdPartyLibraries(dexResults: List<DexAnalysisResult>, nativeLibNames: List<String>): List<Pair<String, String>> {
    val results = mutableListOf<Pair<String, String>>()
    val packages = dexResults.flatMap { it.samplePackages }
    val classes = dexResults.flatMap { it.sampleClasses }

    fun hasPkg(prefix: String) = packages.any { it.startsWith(prefix) } || classes.any { it.startsWith(prefix) }
    fun hasLib(name: String) = nativeLibNames.any { it.contains(name, ignoreCase = true) }

    if (hasPkg("androidx.compose")) results.add(Pair("Jetpack Compose", "Modern declarative Android UI framework"))
    if (hasPkg("kotlinx.coroutines")) results.add(Pair("Kotlin Coroutines", "Asynchronous and concurrent programming library"))
    if (hasPkg("okhttp3")) results.add(Pair("Square OkHttp", "HTTP & HTTP/2 client for Android"))
    if (hasPkg("retrofit2")) results.add(Pair("Square Retrofit", "Type-safe HTTP client for Android"))
    if (hasPkg("com.google.firebase")) results.add(Pair("Google Firebase SDK", "Cloud backend and mobile services"))
    if (hasPkg("com.google.android.gms")) results.add(Pair("Google Play Services", "Google Play APIs and client libraries"))
    if (hasPkg("io.coil")) results.add(Pair("Coil", "Image loading library for Kotlin & Compose"))
    if (hasPkg("com.bumptech.glide")) results.add(Pair("Glide", "Media management and image loading framework"))
    if (hasPkg("com.facebook.react") || hasLib("libreactnativejni.so")) results.add(Pair("React Native", "Cross-platform JavaScript framework"))
    if (hasPkg("io.flutter") || hasLib("libflutter.so")) results.add(Pair("Flutter", "Google multi-platform UI toolkit"))
    if (hasPkg("com.unity3d") || hasLib("libunity.so")) results.add(Pair("Unity Engine", "3D game engine and runtime"))

    if (results.isEmpty()) {
        results.add(Pair("Android Framework Core", "Komponen dasar Android SDK"))
    }
    return results
}
