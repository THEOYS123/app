package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.logging.AppLogger
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.ApkExplorerScreen
import com.example.ui.screens.AppSelectorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DexAnalyzerScreen
import com.example.ui.screens.FileEditorScreen
import com.example.ui.screens.FileViewerScreen
import com.example.ui.screens.LogMonitorScreen
import com.example.ui.screens.PermissionCenterScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.CyanBright
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set global uncaught exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLogger.c("UncaughtException", "Crash in thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        setContent {
            MyApplicationTheme(darkTheme = true) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val currentArchive by viewModel.currentArchive.collectAsState()

    var showDrawerMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // BackHandler support
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        viewModel.handleBack()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "DEEP APK INSPECTOR",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = CyanBright,
                                letterSpacing = 1.sp
                            )
                            if (currentArchive != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(EmeraldBright.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = currentArchive!!.name.take(16),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldBright
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDrawerMenu = true },
                        modifier = Modifier.testTag("btn_menu_drawer")
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = CyanBright)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate900,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Slate900,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(1.dp, Slate800)
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.DASHBOARD,
                    onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 10.sp) },
                    colors = navigationItemColors(),
                    modifier = Modifier.testTag("nav_item_dashboard")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.APP_SELECTOR,
                    onClick = { viewModel.navigateTo(AppScreen.APP_SELECTOR) },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Apps") },
                    label = { Text("Apps", fontSize = 10.sp) },
                    colors = navigationItemColors(),
                    modifier = Modifier.testTag("nav_item_apps")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.EXPLORER,
                    onClick = { viewModel.navigateTo(AppScreen.EXPLORER) },
                    icon = { Icon(Icons.Default.Storage, contentDescription = "Explorer") },
                    label = { Text("Explorer", fontSize = 10.sp) },
                    colors = navigationItemColors(),
                    modifier = Modifier.testTag("nav_item_explorer")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.SEARCH,
                    onClick = { viewModel.navigateTo(AppScreen.SEARCH) },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search", fontSize = 10.sp) },
                    colors = navigationItemColors(),
                    modifier = Modifier.testTag("nav_item_search")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.WORKSPACE,
                    onClick = { viewModel.navigateTo(AppScreen.WORKSPACE) },
                    icon = { Icon(Icons.Default.Workspaces, contentDescription = "Workspace") },
                    label = { Text("Workspace", fontSize = 10.sp) },
                    colors = navigationItemColors(),
                    modifier = Modifier.testTag("nav_item_workspace")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate950)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel, onNavigate = { viewModel.navigateTo(it) })
                AppScreen.APP_SELECTOR -> AppSelectorScreen(viewModel = viewModel)
                AppScreen.EXPLORER -> ApkExplorerScreen(viewModel = viewModel)
                AppScreen.SEARCH -> SearchScreen(viewModel = viewModel)
                AppScreen.VIEWER -> FileViewerScreen(viewModel = viewModel)
                AppScreen.EDITOR -> FileEditorScreen(viewModel = viewModel)
                AppScreen.DEX_ANALYZER -> DexAnalyzerScreen(viewModel = viewModel)
                AppScreen.PERMISSIONS -> PermissionCenterScreen(viewModel = viewModel)
                AppScreen.WORKSPACE -> WorkspaceScreen(viewModel = viewModel)
                AppScreen.LOGS -> LogMonitorScreen()
                AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }

            // Global Loading Overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.border(1.dp, CyanBright.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(color = CyanBright, modifier = Modifier.size(36.dp))
                            Text(
                                text = statusMessage ?: "Sedang memproses...",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Status message toast
            if (statusMessage != null && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyanBright.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = statusMessage!!,
                                color = Slate200,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearStatusMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Drawer Sheet for Advanced Tools
    if (showDrawerMenu) {
        ModalBottomSheet(
            onDismissRequest = { showDrawerMenu = false },
            sheetState = sheetState,
            containerColor = Slate900
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "MENU ALAT & ANALISIS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = CyanBright,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                DrawerMenuItem(
                    icon = Icons.Default.Description,
                    title = "DEX & Native Analyzer",
                    subtitle = "Inspeksi bytecode DEX, class list, dan library .so",
                    onClick = {
                        showDrawerMenu = false
                        viewModel.navigateTo(AppScreen.DEX_ANALYZER)
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.Workspaces,
                    title = "Modification Workspace",
                    subtitle = "Kelola file hasil ekstraksi dan ekspor ZIP",
                    onClick = {
                        showDrawerMenu = false
                        viewModel.navigateTo(AppScreen.WORKSPACE)
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.Lock,
                    title = "Permission Center",
                    subtitle = "Status izin Android & transparansi sandbox",
                    onClick = {
                        showDrawerMenu = false
                        viewModel.navigateTo(AppScreen.PERMISSIONS)
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.BugReport,
                    title = "Live Error & Activity Monitor",
                    subtitle = "Log peristiwa sistem dan stack trace",
                    onClick = {
                        showDrawerMenu = false
                        viewModel.navigateTo(AppScreen.LOGS)
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Pengaturan Aplikasi",
                    subtitle = "Konfigurasi parser, cache, dan informasi aplikasi",
                    onClick = {
                        showDrawerMenu = false
                        viewModel.navigateTo(AppScreen.SETTINGS)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Slate850)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = CyanBright, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
            Text(text = subtitle, fontSize = 11.sp, color = Slate400)
        }
    }
}

@Composable
fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Slate950,
    selectedTextColor = CyanBright,
    indicatorColor = CyanBright,
    unselectedIconColor = Slate400,
    unselectedTextColor = Slate400
)
