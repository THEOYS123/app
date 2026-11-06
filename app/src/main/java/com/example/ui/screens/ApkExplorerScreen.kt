package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.archive.ApkArchiveManager
import com.example.core.model.ApkFileEntry
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.BlueNeon
import com.example.ui.theme.CyanBright
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldNeon
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
import kotlinx.coroutines.launch

@Composable
fun ApkExplorerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentArchive by viewModel.currentArchive.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val entries by viewModel.currentDirectoryEntries.collectAsState()
    val category by viewModel.explorerCategory.collectAsState()
    val sortBy by viewModel.explorerSortBy.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val selectedEntries by viewModel.selectedEntries.collectAsState()

    var detailEntry by remember { mutableStateOf<ApkFileEntry?>(null) }
    var entrySha256 by remember { mutableStateOf("") }

    // SAF Create Document for Exporting single file
    var exportTargetEntry by remember { mutableStateOf<ApkFileEntry?>(null) }
    val exportSingleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null && exportTargetEntry != null) {
            context.contentResolver.openOutputStream(uri)?.let { output ->
                viewModel.exportSingleFile(exportTargetEntry!!.path, output)
            }
        }
    }

    if (currentArchive == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Slate950)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = Slate600, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Belum Ada APK Yang Dipilih",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Silakan pilih aplikasi terinstal atau buka file APK terlebih dahulu melalui menu Apps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // Top Toolbar: Archive Title & Breadcrumbs
        SurfaceHeader(
            archiveName = currentArchive!!.name,
            totalEntries = currentArchive!!.totalEntries,
            currentPath = currentPath,
            onNavigateUp = { viewModel.navigateUpDirectory() },
            onBreadcrumbClick = { viewModel.openDirectory(it) }
        )

        // Filters, Sort & Mode Toolbar
        ExplorerToolbar(
            category = category,
            onCategoryChange = { viewModel.setExplorerCategory(it) },
            sortBy = sortBy,
            onSortChange = { viewModel.setExplorerSort(it) },
            isGridView = isGridView,
            onToggleGrid = { viewModel.toggleViewMode() },
            selectedCount = selectedEntries.size,
            onSelectAll = { viewModel.selectAllEntries() },
            onClearSelection = { viewModel.clearSelectedEntries() }
        )

        // File/Folder Content Area
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Direktori ini kosong atau tidak ada file yang cocok dengan filter '$category'.",
                    color = Slate400,
                    fontSize = 13.sp
                )
            }
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.path }) { entry ->
                    EntryGridCard(
                        entry = entry,
                        isSelected = selectedEntries.contains(entry.path),
                        onClick = { viewModel.viewEntry(entry) },
                        onSelectToggle = { viewModel.toggleSelectEntry(entry.path) },
                        onShowDetail = {
                            detailEntry = entry
                            coroutineScope.launch {
                                entrySha256 = ApkArchiveManager.computeEntrySha256(entry.path)
                            }
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(entries, key = { it.path }) { entry ->
                    EntryListCard(
                        entry = entry,
                        isSelected = selectedEntries.contains(entry.path),
                        onClick = { viewModel.viewEntry(entry) },
                        onSelectToggle = { viewModel.toggleSelectEntry(entry.path) },
                        onExtractWorkspace = { viewModel.extractEntryToWorkspace(entry.path) },
                        onExport = {
                            exportTargetEntry = entry
                            exportSingleLauncher.launch(entry.name)
                        },
                        onShowDetail = {
                            detailEntry = entry
                            coroutineScope.launch {
                                entrySha256 = ApkArchiveManager.computeEntrySha256(entry.path)
                            }
                        }
                    )
                }
            }
        }

        // Selection Action Bar (if items selected)
        if (selectedEntries.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate900)
                    .border(1.dp, Slate800)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedEntries.size} entri dipilih",
                    fontWeight = FontWeight.Bold,
                    color = CyanBright,
                    fontSize = 13.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberNeon)
                            .clickable {
                                selectedEntries.forEach { viewModel.extractEntryToWorkspace(it) }
                                viewModel.clearSelectedEntries()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Ekstrak ke Workspace", color = Slate950, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate800)
                            .clickable { viewModel.clearSelectedEntries() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Batal", color = Slate200, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // Detail Dialog
    if (detailEntry != null) {
        val entry = detailEntry!!
        AlertDialog(
            onDismissRequest = { detailEntry = null },
            containerColor = Slate900,
            title = {
                Text(text = entry.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetailRow("Path Lengkap", entry.path)
                    DetailRow("Jenis", if (entry.isDirectory) "Direktori" else entry.mimeType)
                    DetailRow("Ukuran Asli", "${entry.sizeBytes} byte (${entry.sizeBytes / 1024} KB)")
                    DetailRow("Ukuran Terkompresi", "${entry.compressedSizeBytes} byte")
                    DetailRow("CRC-32", "0x%08X".format(entry.crc32))
                    DetailRow("File Tersembunyi", if (entry.isHidden) "Ya (Dimulai titik)" else "Tidak")
                    if (entrySha256.isNotEmpty()) {
                        DetailRow("SHA-256", entrySha256)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailEntry = null }) {
                    Text("Tutup", color = CyanBright)
                }
            }
        )
    }
}

@Composable
fun SurfaceHeader(
    archiveName: String,
    totalEntries: Int,
    currentPath: String,
    onNavigateUp: () -> Unit,
    onBreadcrumbClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate900)
            .border(1.dp, Slate800)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = archiveName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$totalEntries file",
                fontSize = 12.sp,
                color = CyanBright,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Breadcrumbs Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentPath.isNotEmpty()) {
                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up", tint = CyanBright, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Root button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (currentPath.isEmpty()) CyanBright.copy(alpha = 0.2f) else Slate800)
                    .clickable { onBreadcrumbClick("") }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "root",
                    fontSize = 12.sp,
                    color = if (currentPath.isEmpty()) CyanBright else Slate200,
                    fontWeight = FontWeight.Bold
                )
            }

            // Subdirectories
            val segments = currentPath.split('/').filter { it.isNotBlank() }
            var accumulated = ""
            for ((index, segment) in segments.withIndex()) {
                accumulated = if (accumulated.isEmpty()) segment else "$accumulated/$segment"
                val thisPath = accumulated
                val isLast = index == segments.size - 1

                Icon(
                    Icons.Default.ArrowForwardIos,
                    contentDescription = null,
                    tint = Slate600,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(10.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isLast) CyanBright.copy(alpha = 0.2f) else Slate800)
                        .clickable { onBreadcrumbClick(thisPath) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = segment,
                        fontSize = 12.sp,
                        color = if (isLast) CyanBright else Slate200,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ExplorerToolbar(
    category: String,
    onCategoryChange: (String) -> Unit,
    sortBy: String,
    onSortChange: (String) -> Unit,
    isGridView: Boolean,
    onToggleGrid: () -> Unit,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate950)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Quick Categories Scrollable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val categories = listOf(
                "ALL" to "Semua",
                "MANIFEST" to "Manifest",
                "DEX" to "DEX",
                "ASSETS" to "Assets",
                "RES" to "Res",
                "LIBS" to "Native (.so)",
                "CONFIGS" to "Configs",
                "META" to "META-INF"
            )
            for ((key, title) in categories) {
                val selected = category == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) CyanBright else Slate900)
                        .border(1.dp, if (selected) CyanBright else Slate800, RoundedCornerShape(6.dp))
                        .clickable { onCategoryChange(key) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Slate950 else Slate200
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // View Mode & Sort Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Urutkan:", fontSize = 11.sp, color = Slate400)
                SortOptionChip("Nama", sortBy == "NAME") { onSortChange("NAME") }
                SortOptionChip("Ukuran", sortBy == "SIZE") { onSortChange("SIZE") }
                SortOptionChip("Tipe", sortBy == "TYPE") { onSortChange("TYPE") }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (selectedCount > 0) onClearSelection() else onSelectAll() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.SelectAll,
                        contentDescription = "Select All",
                        tint = if (selectedCount > 0) AmberNeon else Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onToggleGrid,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "View Mode",
                        tint = CyanBright,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SortOptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) CyanBright else Slate400,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
fun EntryListCard(
    entry: ApkFileEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSelectToggle: () -> Unit,
    onExtractWorkspace: () -> Unit,
    onExport: () -> Unit,
    onShowDetail: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Slate850 else Slate900
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isSelected) CyanBright else Slate800, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("entry_${entry.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelectToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = CyanBright,
                    checkmarkColor = Slate950,
                    uncheckedColor = Slate700
                ),
                modifier = Modifier.size(20.dp)
            )

            // Entry Icon
            val (icon, tint) = getEntryIconAndColor(entry)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }

            // Entry Name & Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = entry.name,
                        fontWeight = if (entry.isDirectory) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (entry.isDirectory) Color.White else Slate200,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.isHidden) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Slate800)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("HIDDEN", fontSize = 8.sp, color = AmberNeon, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (entry.isDirectory) {
                        Text(text = "Folder", fontSize = 11.sp, color = Slate400)
                    } else {
                        Text(
                            text = if (entry.sizeBytes > 1024 * 1024) "${entry.sizeBytes / (1024 * 1024)} MB" else "${entry.sizeBytes / 1024} KB",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                        Text(text = "•", fontSize = 10.sp, color = Slate600)
                        Text(text = entry.extension.uppercase().ifEmpty { "BIN" }, fontSize = 10.sp, color = tint, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Action Buttons
            if (!entry.isDirectory) {
                IconButton(onClick = onExtractWorkspace, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Workspaces, contentDescription = "To Workspace", tint = AmberNeon, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onExport, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.DriveFileMove, contentDescription = "Export", tint = CyanBright, modifier = Modifier.size(16.dp))
                }
            }

            IconButton(onClick = onShowDetail, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Info, contentDescription = "Detail", tint = Slate400, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun EntryGridCard(
    entry: ApkFileEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSelectToggle: () -> Unit,
    onShowDetail: () -> Unit
) {
    val (icon, tint) = getEntryIconAndColor(entry)
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Slate850 else Slate900),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isSelected) CyanBright else Slate800, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry.name,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = if (entry.isDirectory) "Dir" else "${entry.sizeBytes / 1024} KB",
                fontSize = 9.sp,
                color = Slate400
            )
        }
    }
}

fun getEntryIconAndColor(entry: ApkFileEntry): Pair<ImageVector, Color> {
    if (entry.isDirectory) {
        return Pair(Icons.Default.Folder, AmberNeon)
    }
    return when (entry.extension.lowercase()) {
        "xml" -> Pair(Icons.Default.Code, CyanBright)
        "dex" -> Pair(Icons.Default.DataObject, PurpleNeon)
        "so" -> Pair(Icons.Default.Description, RoseNeon)
        "png", "jpg", "jpeg", "webp", "gif" -> Pair(Icons.Default.Image, EmeraldBright)
        "json" -> Pair(Icons.Default.DataObject, AmberNeon)
        else -> Pair(Icons.Default.Description, BlueNeon)
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = label, fontSize = 11.sp, color = Slate400)
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}
