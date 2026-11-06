package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

@Composable
fun FileEditorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val editingPath by viewModel.editingPath.collectAsState()
    val editorContent by viewModel.editorContent.collectAsState()
    val originalContent by viewModel.originalContentForDiff.collectAsState()

    // Undo / Redo history stacks
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }

    var showFindReplace by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }

    var showDiffDialog by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // SAF Save As launcher
    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(editorContent.toByteArray(Charsets.UTF_8))
                Toast.makeText(context, "File berhasil disimpan ke penyimpanan!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // Top Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate900)
                .border(1.dp, Slate800)
                .padding(horizontal = 14.dp, vertical = 8.dp)
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
                            text = editingPath.substringAfterLast('/'),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Workspace Copy • ${editorContent.length} karakter",
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }
                }

                // Action icons: Undo, Redo, Find/Replace, Diff
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                redoStack.add(editorContent)
                                val prev = undoStack.removeAt(undoStack.size - 1)
                                viewModel.updateEditorContent(prev)
                            }
                        },
                        enabled = undoStack.isNotEmpty(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (undoStack.isNotEmpty()) CyanBright else Slate600, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                undoStack.add(editorContent)
                                val next = redoStack.removeAt(redoStack.size - 1)
                                viewModel.updateEditorContent(next)
                            }
                        },
                        enabled = redoStack.isNotEmpty(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (redoStack.isNotEmpty()) CyanBright else Slate600, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { showFindReplace = !showFindReplace },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.FindReplace, contentDescription = "Find/Replace", tint = if (showFindReplace) CyanBright else Slate400, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { showDiffDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Compare, contentDescription = "Compare Diff", tint = PurpleNeon, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Find & Replace Drawer
            if (showFindReplace) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = findText,
                        onValueChange = { findText = it },
                        placeholder = { Text("Cari...", color = Slate400, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanBright,
                            unfocusedBorderColor = Slate700,
                            focusedContainerColor = Slate850,
                            unfocusedContainerColor = Slate850,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = replaceText,
                        onValueChange = { replaceText = it },
                        placeholder = { Text("Ganti dengan...", color = Slate400, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanBright,
                            unfocusedBorderColor = Slate700,
                            focusedContainerColor = Slate850,
                            unfocusedContainerColor = Slate850,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyanBright)
                            .clickable {
                                if (findText.isNotEmpty()) {
                                    undoStack.add(editorContent)
                                    val replaced = editorContent.replace(findText, replaceText)
                                    viewModel.updateEditorContent(replaced)
                                    Toast.makeText(context, "Selesai mengganti", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Text("Ganti", fontSize = 11.sp, color = Slate950, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Integrity & Signing Explanation Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate850)
                .border(1.dp, AmberNeon.copy(alpha = 0.3f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AmberNeon, modifier = Modifier.size(16.dp))
                Text(
                    text = "Perubahan disimpan di Workspace lokal. Memodifikasi APK asli memerlukan build ulang dan penandatanganan digital baru (original signature tidak dapat dipertahankan).",
                    fontSize = 10.sp,
                    color = Slate200,
                    lineHeight = 14.sp
                )
            }
        }

        // Validation Results (if validated)
        if (validationResult != null) {
            val (isValid, message) = validationResult!!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isValid) EmeraldBright.copy(alpha = 0.15f) else RoseNeon.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = message,
                    fontSize = 11.sp,
                    color = if (isValid) EmeraldBright else RoseNeon,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Text Editor Area
        TextField(
            value = editorContent,
            onValueChange = { newText ->
                if (editorContent != newText) {
                    if (undoStack.isEmpty() || undoStack.last() != editorContent) {
                        undoStack.add(editorContent)
                        redoStack.clear()
                    }
                    viewModel.updateEditorContent(newText)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("code_editor_field"),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Slate950,
                unfocusedContainerColor = Slate950,
                focusedTextColor = Color.White,
                unfocusedTextColor = Slate200,
                cursorColor = CyanBright
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        )

        // Bottom Action Bar: Validate, Save to Workspace, Save As
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate900)
                .border(1.dp, Slate800)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val ext = editingPath.substringAfterLast('.').lowercase()

            // Validate button for XML / JSON
            if (ext in listOf("xml", "json")) {
                Button(
                    onClick = {
                        validationResult = validateSyntax(editorContent, ext)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("VALIDASI", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { viewModel.saveEditorToWorkspace() },
                colors = ButtonDefaults.buttonColors(containerColor = CyanBright),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("btn_save_workspace")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Slate950, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SIMPAN WORKSPACE", color = Slate950, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { saveAsLauncher.launch(editingPath.substringAfterLast('/')) },
                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SAVE AS (SAF)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Diff Dialog
    if (showDiffDialog) {
        val origLines = originalContent.lines()
        val modLines = editorContent.lines()
        val changed = origLines != modLines

        AlertDialog(
            onDismissRequest = { showDiffDialog = false },
            containerColor = Slate900,
            title = {
                Text("Perbandingan Perubahan (Diff)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(modifier = Modifier.height(300.dp)) {
                    Text(
                        text = if (changed) "Terdapat perubahan antara file asli dan workspace:" else "Tidak ada perubahan (konten sama persis).",
                        fontSize = 12.sp,
                        color = if (changed) AmberNeon else EmeraldBright
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(maxOf(origLines.size, modLines.size)) { i ->
                            val orig = origLines.getOrNull(i)
                            val mod = modLines.getOrNull(i)
                            if (orig != mod) {
                                if (orig != null) {
                                    Text("- $orig", color = RoseNeon, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                if (mod != null) {
                                    Text("+ $mod", color = EmeraldBright, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiffDialog = false }) {
                    Text("Tutup", color = CyanBright)
                }
            }
        )
    }
}

private fun validateSyntax(content: String, ext: String): Pair<Boolean, String> {
    return try {
        if (ext == "json") {
            val trimmed = content.trim()
            if (trimmed.startsWith("{")) {
                JSONObject(trimmed)
            } else if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                throw IllegalArgumentException("JSON harus dimulai dengan { atau [")
            }
            Pair(true, "✓ Format JSON valid!")
        } else if (ext == "xml") {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(content))
            while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {}
            Pair(true, "✓ Format XML valid!")
        } else {
            Pair(true, "Format didukung")
        }
    } catch (e: Exception) {
        Pair(false, "✗ Error Sintaks: ${e.message}")
    }
}
