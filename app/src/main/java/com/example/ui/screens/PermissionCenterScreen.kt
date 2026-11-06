package com.example.ui.screens

import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.PermissionItem
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanBright
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.RoseNeon
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun PermissionCenterScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissions by viewModel.permissionsList.collectAsState()

    // Runtime permission launcher for media
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        viewModel.refreshPermissions()
        val allGranted = permissionsMap.values.all { it }
        if (allGranted) {
            Toast.makeText(context, "Semua izin yang diminta telah diberikan!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Beberapa izin belum diberikan.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyanBright.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanBright.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = CyanBright, modifier = Modifier.size(24.dp))
                        }

                        Column {
                            Text(
                                text = "ANDROID PERMISSION CENTER",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = CyanBright,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Pemeriksaan Izin Sistem & Keterbatasan Sandbox",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestPermissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.READ_MEDIA_IMAGES,
                                            android.Manifest.permission.READ_MEDIA_VIDEO,
                                            android.Manifest.permission.READ_MEDIA_AUDIO
                                        )
                                    )
                                } else {
                                    requestPermissionLauncher.launch(
                                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanBright),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_grant_permissions")
                        ) {
                            Text("MINTA IZIN", color = Slate950, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.openAppSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_open_settings")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Slate200, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PENGATURAN", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.refreshPermissions() },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_recheck_permissions")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CEK ULANG", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Sandbox Security Transparency Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmberNeon.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AmberNeon, modifier = Modifier.size(18.dp))
                        Text(
                            text = "INFORMASI KETERBATASAN SANDBOX ANDROID",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberNeon,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. Android Application Sandbox mengisolasi setiap aplikasi dalam Linux User ID (UID) unik yang terpisah.\n" +
                                "2. Memberikan izin aplikasi TIDAK DAPAT dan TIDAK BOLEH digunakan untuk membuka direktori privat (/data/data/<package>/) aplikasi lain tanpa akses Root (yang tidak didukung demi keamanan sistem).\n" +
                                "3. Deep APK Inspector mengakses struktur aplikasi secara resmi melalui:\n" +
                                "   • Base APK yang terdaftar dan dibagikan secara resmi oleh Android PackageManager;\n" +
                                "   • Storage Access Framework (SAF) dokumen yang diotorisasi secara sadar oleh pengguna;\n" +
                                "   • Direktori Workspace internal aplikasi ini sendiri.\n" +
                                "4. Tidak ada eksploitasi, bypass sandbox, atau pencurian kredensial yang dilakukan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate200,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Permissions Status List
        item {
            Text(
                text = "STATUS IZIN DAN AKSES SISTEM",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 1.sp
            )
        }

        items(permissions) { perm ->
            PermissionCard(perm = perm)
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun PermissionCard(perm: PermissionItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate800, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (perm.isGranted) EmeraldBright.copy(alpha = 0.15f) else RoseNeon.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (perm.isGranted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (perm.isGranted) EmeraldBright else RoseNeon,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = perm.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (perm.isGranted) EmeraldBright.copy(alpha = 0.2f) else RoseNeon.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (perm.isGranted) "DIBERIKAN" else "BELUM ADA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (perm.isGranted) EmeraldBright else RoseNeon
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = perm.permissionString,
                    fontSize = 10.sp,
                    color = Slate400,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = perm.description,
                    fontSize = 11.sp,
                    color = Slate200
                )
            }
        }
    }
}
