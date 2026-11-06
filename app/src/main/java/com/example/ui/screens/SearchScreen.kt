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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ApkFileEntry
import com.example.core.model.SearchResultItem
import com.example.core.model.SearchStatus
import com.example.core.search.ApkSearchEngine
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
fun SearchScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filter by viewModel.searchFilter.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val summary by viewModel.searchSummary.collectAsState()
    val isSearching by ApkSearchEngine.isSearching.collectAsState()
    val searchProgress by ApkSearchEngine.searchProgress.collectAsState()
    val history by ApkSearchEngine.searchHistory.collectAsState()

    var showAdvancedFilters by remember { mutableStateOf(false) }

    // Export launcher
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Main Search Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filter.query,
                    onValueChange = { viewModel.updateSearchFilter(filter.copy(query = it)) },
                    placeholder = { Text("Cari file, folder, atau teks isi...", color = Slate400, fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_query_input"),
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

                Button(
                    onClick = { viewModel.executeSearch() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanBright),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("btn_execute_search")
                ) {
                    Text("CARI", color = Slate950, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Search in Content Toggle & Advanced Filters Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = filter.searchInContent,
                        onCheckedChange = { viewModel.updateSearchFilter(filter.copy(searchInContent = it)) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CyanBright,
                            checkmarkColor = Slate950,
                            uncheckedColor = Slate700
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cari di dalam isi teks (Content Search)",
                        fontSize = 12.sp,
                        color = if (filter.searchInContent) CyanBright else Slate200,
                        fontWeight = if (filter.searchInContent) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showAdvancedFilters = !showAdvancedFilters }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = if (showAdvancedFilters) CyanBright else Slate400, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showAdvancedFilters) "Tutup Filter" else "Filter Lengkap",
                        fontSize = 11.sp,
                        color = if (showAdvancedFilters) CyanBright else Slate400
                    )
                }
            }
        }

        // Extension Quick Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Ekstensi:", fontSize = 11.sp, color = Slate400)
                val extList = listOf("", ".xml", ".dex", ".json", ".so", ".txt", ".png", ".html", ".js")
                for (ext in extList) {
                    val isSelected = (ext.isEmpty() && filter.extension.isEmpty()) || filter.extension.equals(ext.removePrefix("."), true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CyanBright else Slate900)
                            .border(1.dp, if (isSelected) CyanBright else Slate800, RoundedCornerShape(6.dp))
                            .clickable {
                                val clean = ext.removePrefix(".")
                                viewModel.updateSearchFilter(filter.copy(extension = if (isSelected && ext.isNotEmpty()) "" else clean))
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (ext.isEmpty()) "Semua" else ext,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Slate950 else Slate200
                        )
                    }
                }
            }
        }

        // Advanced Filters Panel (Collapsible)
        if (showAdvancedFilters) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("PENGATURAN PENCARIAN TINGKAT LANJUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)

                        // Path filter
                        OutlinedTextField(
                            value = filter.directoryPath,
                            onValueChange = { viewModel.updateSearchFilter(filter.copy(directoryPath = it)) },
                            placeholder = { Text("Filter Path / Direktori (contoh: res/layout)", color = Slate400, fontSize = 12.sp) },
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

                        // Toggle row 1
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            FilterToggle("Regular Expression (Regex)", filter.isRegex) {
                                viewModel.updateSearchFilter(filter.copy(isRegex = it))
                            }
                            FilterToggle("Case Sensitive", filter.isCaseSensitive) {
                                viewModel.updateSearchFilter(filter.copy(isCaseSensitive = it))
                            }
                        }

                        // Toggle row 2
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            FilterToggle("Awalan Nama (Prefix)", filter.matchPrefix) {
                                viewModel.updateSearchFilter(filter.copy(matchPrefix = it))
                            }
                            FilterToggle("Akhiran Nama (Suffix)", filter.matchSuffix) {
                                viewModel.updateSearchFilter(filter.copy(matchSuffix = it))
                            }
                        }

                        // Toggle row 3
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            FilterToggle("Termasuk File Tersembunyi", filter.includeHidden) {
                                viewModel.updateSearchFilter(filter.copy(includeHidden = it))
                            }
                        }
                    }
                }
            }
        }

        // Recent Search History Chips
        if (history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Slate600, modifier = Modifier.size(14.dp))
                    Text("Riwayat:", fontSize = 11.sp, color = Slate400)
                    for (h in history.take(6)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Slate900)
                                .clickable {
                                    viewModel.updateSearchFilter(filter.copy(query = h))
                                    viewModel.executeSearch()
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = h, fontSize = 10.sp, color = CyanBright)
                        }
                    }
                }
            }
        }

        // Progress indicator
        if (isSearching) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Memindai indeks file APK...", fontSize = 11.sp, color = CyanBright)
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { searchProgress },
                        color = CyanBright,
                        trackColor = Slate800,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Search Summary Bar
        if (summary != null) {
            item {
                val sum = summary!!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate900)
                        .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Diperiksa: ${sum.totalScannedFiles} file, ${sum.totalScannedFolders} folder (${sum.durationMs}ms)",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (sum.status) {
                                    SearchStatus.COMPLETED -> EmeraldBright.copy(alpha = 0.2f)
                                    SearchStatus.NO_RESULTS -> AmberNeon.copy(alpha = 0.2f)
                                    else -> RoseNeon.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (sum.matchedCount > 0) "${sum.matchedCount} DITEMUKAN" else sum.status.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sum.matchedCount > 0) EmeraldBright else AmberNeon
                        )
                    }
                }
            }
        }

        // Search Failure Diagnosis Explanation Card (if 0 results)
        if (summary != null && summary!!.matchedCount == 0 && summary!!.explanationReason != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AmberNeon.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AmberNeon, modifier = Modifier.size(20.dp))
                            Text(
                                text = "DIAGNOSIS HASIL PENCARIAN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = AmberNeon,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = summary!!.explanationReason!!,
                            fontSize = 12.sp,
                            color = Slate200,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Search Results List
        if (results.isNotEmpty()) {
            items(results, key = { it.entry.path }) { item ->
                SearchResultCard(
                    item = item,
                    onOpen = { viewModel.viewEntry(item.entry) },
                    onExport = {
                        exportTargetEntry = item.entry
                        exportSingleLauncher.launch(item.entry.name)
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun FilterToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = CyanBright, checkmarkColor = Slate950, uncheckedColor = Slate700),
            modifier = Modifier.size(20.dp)
        )
        Text(text = label, fontSize = 11.sp, color = if (checked) Color.White else Slate400)
    }
}

@Composable
fun SearchResultCard(
    item: SearchResultItem,
    onOpen: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate800, RoundedCornerShape(10.dp))
            .clickable(onClick = onOpen)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.entry.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (item.matchType) {
                                "CONTENT" -> EmeraldBright.copy(alpha = 0.2f)
                                "EXTENSION" -> PurpleNeon.copy(alpha = 0.2f)
                                else -> CyanBright.copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.matchType,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (item.matchType) {
                            "CONTENT" -> EmeraldBright
                            "EXTENSION" -> PurpleNeon
                            else -> CyanBright
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Full path
            Text(
                text = item.entry.path,
                fontSize = 11.sp,
                color = Slate400,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Content snippet if matched in content
            if (item.snippet != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Slate850)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row {
                        if (item.lineNumber != null) {
                            Text(
                                text = "L${item.lineNumber}: ",
                                fontSize = 11.sp,
                                color = AmberNeon,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = item.snippet,
                            fontSize = 11.sp,
                            color = Slate200,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.entry.sizeBytes / 1024} KB • ${item.entry.mimeType}",
                    fontSize = 10.sp,
                    color = Slate400
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyanBright)
                            .clickable(onClick = onOpen)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Buka", fontSize = 11.sp, color = Slate950, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Slate800)
                            .clickable(onClick = onExport)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Ekspor", fontSize = 11.sp, color = Slate200)
                    }
                }
            }
        }
    }
}
