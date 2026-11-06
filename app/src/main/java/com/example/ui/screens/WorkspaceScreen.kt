package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.WorkspaceItem
import com.example.core.workspace.WorkspaceManager
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanBright
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.RoseNeon
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.launch

@Composable
fun WorkspaceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentArchive by viewModel.currentArchive.collectAsState()
    val workspaceItems by viewModel.workspaceItems.collectAsState()

    var itemToDelete by remember { mutableStateOf<WorkspaceItem?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    // Export Workspace as ZIP launcher
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { output ->
                viewModel.exportWorkspaceZip(output)
            }
        }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberNeon.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Workspaces, contentDescription = null, tint = AmberNeon, modifier = Modifier.size(22.dp))
                    }

                    Column {
                        Text(
                            text = "MODIFICATION WORKSPACE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = AmberNeon,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${currentArchive?.name ?: "Belum ada APK"} • ${workspaceItems.size} file terekstrak",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }

                if (workspaceItems.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirmation = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Clear All", tint = RoseNeon)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Export workspace as ZIP
            if (workspaceItems.isNotEmpty()) {
                Button(
                    onClick = {
                        val defaultZipName = "${currentArchive?.name ?: "workspace"}_modified.zip"
                        exportZipLauncher.launch(defaultZipName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanBright),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_export_workspace_zip")
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, tint = Slate950, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EKSPOR SELURUH WORKSPACE (ZIP)", color = Slate950, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Integrity & Signature Requirement Explanation Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.dp, AmberNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AmberNeon, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Semua modifikasi dikelola pada salinan kerja terpisah di Workspace. Setiap perubahan isi paket memerlukan build ulang dan penandatanganan ulang (signing). Tanda tangan digital asli tidak dapat dipertahankan.",
                        fontSize = 11.sp,
                        color = Slate200,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Workspace files list
        if (workspaceItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Workspaces, contentDescription = null, tint = Slate700, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Workspace Masih Kosong", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Buka File Explorer atau Viewer lalu pilih 'Ekstrak ke Workspace' untuk mulai mengelola atau mengedit file.",
                        fontSize = 12.sp,
                        color = Slate400,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(workspaceItems, key = { it.relativePath }) { item ->
                    WorkspaceItemCard(
                        item = item,
                        onOpenEditor = {
                            coroutineScope.launch {
                                val text = WorkspaceManager.getWorkspaceFileText(context, currentArchive?.name ?: "", item.relativePath)
                                if (text != null) {
                                    viewModel.openInEditor(item.relativePath, text)
                                } else {
                                    Toast.makeText(context, "File biner tidak dapat diedit sebagai teks", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDelete = { itemToDelete = item }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Single Item Delete Confirmation Dialog
    if (itemToDelete != null) {
        val item = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = Slate900,
            title = {
                Text("Konfirmasi Hapus File", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Apakah Anda yakin ingin menghapus file berikut dari workspace?", color = Slate200, fontSize = 13.sp)
                    DetailRow("Nama File", item.relativePath.substringAfterLast('/'))
                    DetailRow("Path", item.relativePath)
                    DetailRow("Ukuran", "${item.sizeBytes} byte")
                    Text("PERINGATAN: File di workspace yang dihapus tidak dapat dipulihkan.", color = RoseNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorkspaceItem(item.relativePath)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseNeon)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal", color = Slate400)
                }
            }
        )
    }

    // Clear Workspace Confirmation Dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            containerColor = Slate900,
            title = {
                Text("Bersihkan Seluruh Workspace", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Semua file (${workspaceItems.size} file) yang ada dalam workspace '${currentArchive?.name}' akan dihapus secara permanen. Apakah Anda yakin?",
                    color = Slate200,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCurrentWorkspace()
                        showClearConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseNeon)
                ) {
                    Text("Bersihkan Semua", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Batal", color = Slate400)
                }
            }
        )
    }
}

@Composable
fun WorkspaceItemCard(
    item: WorkspaceItem,
    onOpenEditor: () -> Unit,
    onDelete: () -> Unit
) {
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
                    .background(CyanBright.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = CyanBright, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.relativePath.substringAfterLast('/'),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.relativePath} • ${item.sizeBytes / 1024} KB",
                    fontSize = 11.sp,
                    color = Slate400,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val isEditable = item.extension in listOf("xml", "json", "txt", "html", "htm", "css", "js", "properties", "cfg", "ini", "md")

            if (isEditable) {
                IconButton(onClick = onOpenEditor, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CyanBright, modifier = Modifier.size(18.dp))
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseNeon, modifier = Modifier.size(18.dp))
            }
        }
    }
}
