package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material.icons.filled.Workspaces
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.archive.ApkArchiveManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FileViewerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val entry by viewModel.viewingEntry.collectAsState()
    val viewerText by viewModel.viewerText.collectAsState()
    val hexDump by viewModel.viewerHexDump.collectAsState()
    val activeTab by viewModel.viewerTab.collectAsState()
    val isBinaryXmlDecoded by viewModel.isBinaryXmlDecoded.collectAsState()

    var wordWrap by remember { mutableStateOf(false) }
    var inPageSearchQuery by remember { mutableStateOf("") }
    var showInPageSearch by remember { mutableStateOf(false) }
    var imageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var sha256Hash by remember { mutableStateOf("") }

    LaunchedEffect(entry) {
        if (entry != null) {
            val e = entry!!
            if (e.extension in listOf("png", "jpg", "jpeg", "webp", "gif")) {
                val bytes = ApkArchiveManager.readEntryBytes(e.path).getOrNull()
                if (bytes != null) {
                    withContext(Dispatchers.IO) {
                        try {
                            imageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (_: Exception) {}
                    }
                }
            }
            sha256Hash = ApkArchiveManager.computeEntrySha256(e.path)
        }
    }

    // Export single file
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null && entry != null) {
            context.contentResolver.openOutputStream(uri)?.let { output ->
                viewModel.exportSingleFile(entry!!.path, output)
            }
        }
    }

    if (entry == null) {
        Box(modifier = modifier.fillMaxSize().background(Slate950), contentAlignment = Alignment.Center) {
            Text("Tidak ada file yang sedang dilihat", color = Slate400)
        }
        return
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
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { viewModel.handleBack() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanBright, modifier = Modifier.size(18.dp))
                    }

                    Column {
                        Text(
                            text = entry!!.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "${entry!!.sizeBytes / 1024} KB • ${entry!!.path}",
                            fontSize = 11.sp,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showInPageSearch = !showInPageSearch },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Find", tint = if (showInPageSearch) CyanBright else Slate400, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { wordWrap = !wordWrap },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.WrapText, contentDescription = "Wrap", tint = if (wordWrap) CyanBright else Slate400, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            val textToCopy = if (activeTab == "HEX") hexDump else viewerText
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            Toast.makeText(context, "Konten disalin ke clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanBright, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // In-page search bar
            if (showInPageSearch) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = inPageSearchQuery,
                    onValueChange = { inPageSearchQuery = it },
                    placeholder = { Text("Cari teks dalam file ini...", color = Slate400, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate700,
                        focusedContainerColor = Slate850,
                        unfocusedContainerColor = Slate850,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Slate200
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Binary XML Banner Notice (if decoded)
        if (isBinaryXmlDecoded && activeTab == "TEXT") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate850)
                    .border(1.dp, CyanBright.copy(alpha = 0.3f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "✓ Android Binary XML berhasil didekompilasi ke XML teks terbaca secara otomatis",
                    fontSize = 11.sp,
                    color = CyanBright,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Tabs Row: TEXT / HEX / METADATA / IMAGE
        val tabs = listOf("TEXT" to "Teks / XML", "HEX" to "Hex Viewer", "METADATA" to "Metadata") +
                if (entry!!.extension in listOf("png", "jpg", "jpeg", "webp", "gif")) listOf("IMAGE" to "Preview Gambar") else emptyList()

        val tabIndex = tabs.indexOfFirst { it.first == activeTab }.let { if (it >= 0) it else 0 }

        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = Slate900,
            contentColor = CyanBright,
            indicator = { tabPositions ->
                if (tabIndex in tabPositions.indices) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        color = CyanBright
                    )
                }
            }
        ) {
            tabs.forEach { (key, label) ->
                Tab(
                    selected = activeTab == key,
                    onClick = { viewModel.setViewerTab(key) },
                    text = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (activeTab == key) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Slate950)
        ) {
            when (activeTab) {
                "TEXT" -> {
                    val lines = remember(viewerText) { viewerText.lines() }
                    val filteredLines = remember(lines, inPageSearchQuery) {
                        if (inPageSearchQuery.isBlank()) lines
                        else lines.filter { it.contains(inPageSearchQuery, ignoreCase = true) }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        itemsIndexed(filteredLines) { index, line ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (!wordWrap) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
                                    .padding(vertical = 1.dp)
                            ) {
                                Text(
                                    text = "%4d ".format(index + 1),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Slate600
                                )
                                Text(
                                    text = line,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (inPageSearchQuery.isNotBlank() && line.contains(inPageSearchQuery, ignoreCase = true)) AmberNeon else Slate200,
                                    fontWeight = if (inPageSearchQuery.isNotBlank() && line.contains(inPageSearchQuery, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                "HEX" -> {
                    val hexLines = remember(hexDump) { hexDump.lines() }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        items(hexLines) { line ->
                            Text(
                                text = line,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate200,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }
                "METADATA" -> {
                    val e = entry!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, Slate800, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("INTEGRITAS & HASH FILE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyanBright)
                                DetailRow("Nama File", e.name)
                                DetailRow("Path di APK", e.path)
                                DetailRow("MIME Type", e.mimeType)
                                DetailRow("Ukuran Asli", "${e.sizeBytes} byte (${e.sizeBytes / 1024} KB)")
                                DetailRow("Ukuran Terkompresi", "${e.compressedSizeBytes} byte")
                                val ratio = if (e.sizeBytes > 0) ((1f - e.compressedSizeBytes.toFloat() / e.sizeBytes) * 100).toInt() else 0
                                DetailRow("Rasio Kompresi", "$ratio%")
                                DetailRow("CRC-32", "0x%08X".format(e.crc32))
                                DetailRow("SHA-256", sha256Hash.ifEmpty { "Menghitung..." })
                            }
                        }
                    }
                }
                "IMAGE" -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap!!.asImageBitmap(),
                                contentDescription = "Preview Image",
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Slate900)
                                    .border(1.dp, Slate700, RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            )
                        } else {
                            Text("Tidak dapat merender bitmap gambar", color = Slate400)
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate900)
                .border(1.dp, Slate800)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isEditable = entry!!.extension in listOf("xml", "json", "txt", "html", "htm", "css", "js", "properties", "cfg", "ini", "md")

            if (isEditable) {
                Button(
                    onClick = { viewModel.openInEditor(entry!!.path, viewerText) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanBright),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("btn_edit_file")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Slate950, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EDIT DI WORKSPACE", color = Slate950, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Button(
                onClick = { viewModel.extractEntryToWorkspace(entry!!.path) },
                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("btn_extract_file")
            ) {
                Icon(Icons.Default.Workspaces, contentDescription = null, tint = AmberNeon, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("EKSTRAK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            Button(
                onClick = { exportLauncher.launch(entry!!.name) },
                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("btn_export_file")
            ) {
                Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("EKSPOR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}
