package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.archive.ApkArchiveManager
import com.example.core.logging.AppLogger
import com.example.core.model.ApkArchiveInfo
import com.example.core.model.ApkFileEntry
import com.example.core.model.AppInfo
import com.example.core.model.DexAnalysisResult
import com.example.core.model.SearchFilter
import com.example.core.model.SearchResultItem
import com.example.core.model.SearchStatus
import com.example.core.model.SearchSummary
import com.example.core.model.WorkspaceItem
import com.example.core.parser.AndroidBinaryXmlParser
import com.example.core.parser.DexHeaderParser
import com.example.core.scanner.AppScanner
import com.example.core.search.ApkSearchEngine
import com.example.core.workspace.WorkspaceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

enum class AppScreen {
    DASHBOARD,
    APP_SELECTOR,
    EXPLORER,
    SEARCH,
    VIEWER,
    EDITOR,
    DEX_ANALYZER,
    PERMISSIONS,
    WORKSPACE,
    LOGS,
    SETTINGS
}

data class PermissionItem(
    val name: String,
    val permissionString: String,
    val description: String,
    val isGranted: Boolean,
    val isRuntime: Boolean,
    val category: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "MainViewModel"

    // Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val backStack = mutableListOf<AppScreen>()

    // Loading / Status
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Apps List
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _appFilterType = MutableStateFlow("ALL") // "ALL", "USER", "SYSTEM"
    val appFilterType: StateFlow<String> = _appFilterType.asStateFlow()

    private val _appSearchQuery = MutableStateFlow("")
    val appSearchQuery: StateFlow<String> = _appSearchQuery.asStateFlow()

    // Current APK Archive
    private val _currentArchive = MutableStateFlow<ApkArchiveInfo?>(null)
    val currentArchive: StateFlow<ApkArchiveInfo?> = _currentArchive.asStateFlow()

    // Explorer State
    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _currentDirectoryEntries = MutableStateFlow<List<ApkFileEntry>>(emptyList())
    val currentDirectoryEntries: StateFlow<List<ApkFileEntry>> = _currentDirectoryEntries.asStateFlow()

    private val _explorerCategory = MutableStateFlow("ALL")
    val explorerCategory: StateFlow<String> = _explorerCategory.asStateFlow()

    private val _explorerSortBy = MutableStateFlow("NAME") // "NAME", "SIZE", "TYPE"
    val explorerSortBy: StateFlow<String> = _explorerSortBy.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _selectedEntries = MutableStateFlow<Set<String>>(emptySet())
    val selectedEntries: StateFlow<Set<String>> = _selectedEntries.asStateFlow()

    // Viewer State
    private val _viewingEntry = MutableStateFlow<ApkFileEntry?>(null)
    val viewingEntry: StateFlow<ApkFileEntry?> = _viewingEntry.asStateFlow()

    private val _viewerText = MutableStateFlow("")
    val viewerText: StateFlow<String> = _viewerText.asStateFlow()

    private val _viewerHexDump = MutableStateFlow("")
    val viewerHexDump: StateFlow<String> = _viewerHexDump.asStateFlow()

    private val _viewerTab = MutableStateFlow("TEXT") // "TEXT", "HEX", "METADATA", "IMAGE"
    val viewerTab: StateFlow<String> = _viewerTab.asStateFlow()

    private val _isBinaryXmlDecoded = MutableStateFlow(false)
    val isBinaryXmlDecoded: StateFlow<Boolean> = _isBinaryXmlDecoded.asStateFlow()

    // Editor State
    private val _editingPath = MutableStateFlow("")
    val editingPath: StateFlow<String> = _editingPath.asStateFlow()

    private val _editorContent = MutableStateFlow("")
    val editorContent: StateFlow<String> = _editorContent.asStateFlow()

    private val _originalContentForDiff = MutableStateFlow("")
    val originalContentForDiff: StateFlow<String> = _originalContentForDiff.asStateFlow()

    // Search State
    private val _searchFilter = MutableStateFlow(SearchFilter())
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()

    private val _searchSummary = MutableStateFlow<SearchSummary?>(null)
    val searchSummary: StateFlow<SearchSummary?> = _searchSummary.asStateFlow()

    // DEX Analyzer State
    private val _dexResults = MutableStateFlow<List<DexAnalysisResult>>(emptyList())
    val dexResults: StateFlow<List<DexAnalysisResult>> = _dexResults.asStateFlow()

    // Workspace State
    private val _workspaceItems = MutableStateFlow<List<WorkspaceItem>>(emptyList())
    val workspaceItems: StateFlow<List<WorkspaceItem>> = _workspaceItems.asStateFlow()

    // Permissions State
    private val _permissionsList = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissionsList: StateFlow<List<PermissionItem>> = _permissionsList.asStateFlow()

    // Stats
    private val _totalApksAnalyzedCount = MutableStateFlow(0)
    val totalApksAnalyzedCount: StateFlow<Int> = _totalApksAnalyzedCount.asStateFlow()

    init {
        refreshInstalledApps()
        refreshPermissions()
    }

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            backStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun handleBack(): Boolean {
        if (backStack.isNotEmpty()) {
            _currentScreen.value = backStack.removeAt(backStack.size - 1)
            return true
        }
        return false
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Installed Apps
    fun refreshInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Memindai aplikasi yang terinstal..."
            val apps = AppScanner.getInstalledApps(getApplication())
            _installedApps.value = apps
            _isLoading.value = false
            _statusMessage.value = null
        }
    }

    fun setAppFilter(filter: String) {
        _appFilterType.value = filter
    }

    fun setAppSearch(query: String) {
        _appSearchQuery.value = query
    }

    // Inspect APK
    fun inspectInstalledApp(app: AppInfo) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Membuka ${app.appName} (${app.packageName})..."
            val result = ApkArchiveManager.openFromPath(getApplication(), app.apkPath, app.appName)
            result.fold(
                onSuccess = { info ->
                    _currentArchive.value = info
                    _totalApksAnalyzedCount.value++
                    _currentPath.value = ""
                    refreshExplorerEntries()
                    refreshWorkspace()
                    analyzeDex()
                    _isLoading.value = false
                    _statusMessage.value = "Berhasil memuat ${info.name}: ${info.totalEntries} entri."
                    navigateTo(AppScreen.EXPLORER)
                },
                onFailure = { error ->
                    _isLoading.value = false
                    _statusMessage.value = "Gagal membuka APK: ${error.message}"
                }
            )
        }
    }

    fun inspectApkUri(uri: Uri, displayName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Membaca arsip APK dari penyimpanan..."
            val result = ApkArchiveManager.openFromUri(getApplication(), uri, displayName)
            result.fold(
                onSuccess = { info ->
                    _currentArchive.value = info
                    _totalApksAnalyzedCount.value++
                    _currentPath.value = ""
                    refreshExplorerEntries()
                    refreshWorkspace()
                    analyzeDex()
                    _isLoading.value = false
                    _statusMessage.value = "Berhasil memuat APK: ${info.totalEntries} entri."
                    navigateTo(AppScreen.EXPLORER)
                },
                onFailure = { error ->
                    _isLoading.value = false
                    _statusMessage.value = "Gagal memproses file APK: ${error.message}"
                }
            )
        }
    }

    // Explorer Navigation
    fun openDirectory(path: String) {
        _currentPath.value = path
        refreshExplorerEntries()
    }

    fun navigateUpDirectory() {
        val path = _currentPath.value.trim('/')
        if (path.isEmpty()) return
        val lastSlash = path.lastIndexOf('/')
        val newPath = if (lastSlash == -1) "" else path.substring(0, lastSlash)
        openDirectory(newPath)
    }

    fun refreshExplorerEntries() {
        val raw = ApkArchiveManager.getDirectoryEntries(_currentPath.value)
        val filtered = when (_explorerCategory.value) {
            "MANIFEST" -> raw.filter { it.name.equals("AndroidManifest.xml", true) }
            "DEX" -> raw.filter { it.name.endsWith(".dex", true) || it.name == "classes.dex" }
            "ASSETS" -> raw.filter { it.path.startsWith("assets") }
            "RES" -> raw.filter { it.path.startsWith("res") || it.name == "resources.arsc" }
            "LIBS" -> raw.filter { it.path.startsWith("lib") || it.name.endsWith(".so", true) }
            "META" -> raw.filter { it.path.startsWith("META-INF") }
            "CONFIGS" -> raw.filter { it.extension in listOf("json", "xml", "properties", "cfg") }
            else -> raw
        }

        val sorted = when (_explorerSortBy.value) {
            "SIZE" -> filtered.sortedWith(compareBy({ !it.isDirectory }, { -it.sizeBytes }))
            "TYPE" -> filtered.sortedWith(compareBy({ !it.isDirectory }, { it.extension }))
            else -> filtered.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        }

        _currentDirectoryEntries.value = sorted
    }

    fun setExplorerCategory(cat: String) {
        _explorerCategory.value = cat
        refreshExplorerEntries()
    }

    fun setExplorerSort(sort: String) {
        _explorerSortBy.value = sort
        refreshExplorerEntries()
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun toggleSelectEntry(path: String) {
        val current = _selectedEntries.value.toMutableSet()
        if (current.contains(path)) current.remove(path) else current.add(path)
        _selectedEntries.value = current
    }

    fun selectAllEntries() {
        _selectedEntries.value = _currentDirectoryEntries.value.map { it.path }.toSet()
    }

    fun clearSelectedEntries() {
        _selectedEntries.value = emptySet()
    }

    // View File
    fun viewEntry(entry: ApkFileEntry) {
        if (entry.isDirectory) {
            openDirectory(entry.path)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _viewingEntry.value = entry
            _statusMessage.value = "Membaca file ${entry.name}..."

            val bytes = ApkArchiveManager.readEntryBytes(entry.path).getOrNull()
            if (bytes == null) {
                _isLoading.value = false
                _statusMessage.value = "Gagal membaca konten file."
                return@launch
            }

            // Generate Hex Dump
            _viewerHexDump.value = generateHexDump(bytes)

            // Text / XML Decoding
            if (entry.extension == "xml" && AndroidBinaryXmlParser.isBinaryXml(bytes)) {
                _isBinaryXmlDecoded.value = true
                _viewerText.value = AndroidBinaryXmlParser.parse(bytes)
                _viewerTab.value = "TEXT"
            } else if (entry.extension in listOf("xml", "json", "txt", "html", "htm", "css", "js", "properties", "cfg", "ini", "md", "sh")) {
                _isBinaryXmlDecoded.value = false
                _viewerText.value = String(bytes, Charsets.UTF_8)
                _viewerTab.value = "TEXT"
            } else if (entry.extension in listOf("png", "jpg", "jpeg", "webp", "gif")) {
                _viewerTab.value = "IMAGE"
                _viewerText.value = "[Gambar biner: ${bytes.size} byte]"
            } else {
                _viewerTab.value = "HEX"
                _viewerText.value = "[File biner / ${entry.mimeType}]"
            }

            _isLoading.value = false
            _statusMessage.value = null
            navigateTo(AppScreen.VIEWER)
        }
    }

    fun setViewerTab(tab: String) {
        _viewerTab.value = tab
    }

    private fun generateHexDump(bytes: ByteArray, maxLines: Int = 300): String {
        val sb = StringBuilder()
        val limit = minOf(bytes.size, maxLines * 16)
        for (i in 0 until limit step 16) {
            sb.append("%08X  ".format(i))
            // Hex bytes
            for (j in 0 until 16) {
                if (i + j < limit) {
                    sb.append("%02X ".format(bytes[i + j]))
                } else {
                    sb.append("   ")
                }
                if (j == 7) sb.append(" ")
            }
            sb.append(" |")
            // ASCII chars
            for (j in 0 until 16) {
                if (i + j < limit) {
                    val b = bytes[i + j].toInt() and 0xFF
                    val ch = if (b in 32..126) b.toChar() else '.'
                    sb.append(ch)
                }
            }
            sb.append("|\n")
        }
        if (bytes.size > limit) {
            sb.append("\n[... Ditampilkan $limit dari ${bytes.size} byte ...]\n")
        }
        return sb.toString()
    }

    // Editor
    fun openInEditor(path: String, initialContent: String) {
        _editingPath.value = path
        _editorContent.value = initialContent
        _originalContentForDiff.value = initialContent
        navigateTo(AppScreen.EDITOR)
    }

    fun updateEditorContent(content: String) {
        _editorContent.value = content
    }

    fun saveEditorToWorkspace() {
        val apkName = _currentArchive.value?.name ?: "default_apk"
        val path = _editingPath.value
        val content = _editorContent.value

        viewModelScope.launch {
            _isLoading.value = true
            val result = WorkspaceManager.saveFileContent(getApplication(), apkName, path, content)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _statusMessage.value = "Perubahan berhasil disimpan ke Workspace!"
                    refreshWorkspace()
                },
                onFailure = {
                    _statusMessage.value = "Gagal menyimpan ke workspace: ${it.message}"
                }
            )
        }
    }

    // Search
    fun updateSearchFilter(newFilter: SearchFilter) {
        _searchFilter.value = newFilter
    }

    fun executeSearch() {
        viewModelScope.launch {
            val (items, summary) = ApkSearchEngine.executeSearch(_searchFilter.value)
            _searchResults.value = items
            _searchSummary.value = summary
        }
    }

    // DEX Analyzer
    private fun analyzeDex() {
        viewModelScope.launch {
            val entries = ApkArchiveManager.getAllEntries().filter { it.name.endsWith(".dex", true) }
            val list = mutableListOf<DexAnalysisResult>()
            for (e in entries) {
                val bytes = ApkArchiveManager.readEntryBytes(e.path).getOrNull()
                if (bytes != null) {
                    list.add(DexHeaderParser.parse(e.name, bytes))
                }
            }
            _dexResults.value = list
        }
    }

    // Workspace
    fun refreshWorkspace() {
        val apkName = _currentArchive.value?.name ?: return
        viewModelScope.launch {
            val items = WorkspaceManager.getWorkspaceItems(getApplication(), apkName)
            _workspaceItems.value = items
        }
    }

    fun extractEntryToWorkspace(path: String) {
        val apkName = _currentArchive.value?.name ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = WorkspaceManager.extractEntryToWorkspace(getApplication(), apkName, path)
            _isLoading.value = false
            res.fold(
                onSuccess = {
                    _statusMessage.value = "Entri '$path' berhasil diekstrak ke workspace"
                    refreshWorkspace()
                },
                onFailure = {
                    _statusMessage.value = "Gagal ekstrak '$path': ${it.message}"
                }
            )
        }
    }

    fun deleteWorkspaceItem(path: String) {
        val apkName = _currentArchive.value?.name ?: return
        viewModelScope.launch {
            WorkspaceManager.deleteWorkspaceItem(getApplication(), apkName, path)
            refreshWorkspace()
            _statusMessage.value = "Item '$path' dihapus dari workspace"
        }
    }

    fun clearCurrentWorkspace() {
        val apkName = _currentArchive.value?.name ?: return
        viewModelScope.launch {
            WorkspaceManager.clearWorkspace(getApplication(), apkName)
            refreshWorkspace()
            _statusMessage.value = "Workspace dibersihkan"
        }
    }

    fun exportWorkspaceZip(outputStream: OutputStream) {
        val apkName = _currentArchive.value?.name ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = WorkspaceManager.exportWorkspaceAsZip(getApplication(), apkName, outputStream)
            _isLoading.value = false
            res.fold(
                onSuccess = { count ->
                    _statusMessage.value = "Berhasil mengekspor $count file ke format ZIP!"
                },
                onFailure = {
                    _statusMessage.value = "Gagal ekspor: ${it.message}"
                }
            )
        }
    }

    fun exportSingleFile(path: String, outputStream: OutputStream) {
        val apkName = _currentArchive.value?.name ?: "apk_export"
        viewModelScope.launch {
            _isLoading.value = true
            val res = WorkspaceManager.exportSingleFile(getApplication(), apkName, path, outputStream)
            _isLoading.value = false
            res.fold(
                onSuccess = { bytes ->
                    _statusMessage.value = "File '$path' ($bytes byte) berhasil diekspor!"
                },
                onFailure = {
                    _statusMessage.value = "Gagal ekspor file: ${it.message}"
                }
            )
        }
    }

    // Permissions
    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        val list = mutableListOf<PermissionItem>()

        val checkPerm = { p: String ->
            ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
        }

        list.add(
            PermissionItem(
                name = "Package Visibility (Query All Packages)",
                permissionString = "android.permission.QUERY_ALL_PACKAGES",
                description = "Mengizinkan pembacaan daftar aplikasi terinstal pada perangkat untuk diinspeksi.",
                isGranted = true, // declared in manifest
                isRuntime = false,
                category = "Aplikasi Terinstal"
            )
        )

        list.add(
            PermissionItem(
                name = "Storage Access Framework (SAF)",
                permissionString = "SAF Document Provider",
                description = "Akses pemilihan dan ekspor dokumen/APK yang diotorisasi langsung oleh pengguna.",
                isGranted = true,
                isRuntime = false,
                category = "Penyimpanan & File"
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                PermissionItem(
                    name = "Read Images",
                    permissionString = android.Manifest.permission.READ_MEDIA_IMAGES,
                    description = "Membaca asset gambar yang dibagikan dari penyimpanan perangkat.",
                    isGranted = checkPerm(android.Manifest.permission.READ_MEDIA_IMAGES),
                    isRuntime = true,
                    category = "Media"
                )
            )
            list.add(
                PermissionItem(
                    name = "Read Video",
                    permissionString = android.Manifest.permission.READ_MEDIA_VIDEO,
                    description = "Membaca asset video yang dibagikan dari penyimpanan perangkat.",
                    isGranted = checkPerm(android.Manifest.permission.READ_MEDIA_VIDEO),
                    isRuntime = true,
                    category = "Media"
                )
            )
            list.add(
                PermissionItem(
                    name = "Read Audio",
                    permissionString = android.Manifest.permission.READ_MEDIA_AUDIO,
                    description = "Membaca asset suara/audio yang dibagikan dari penyimpanan perangkat.",
                    isGranted = checkPerm(android.Manifest.permission.READ_MEDIA_AUDIO),
                    isRuntime = true,
                    category = "Media"
                )
            )
        } else {
            list.add(
                PermissionItem(
                    name = "Read External Storage",
                    permissionString = android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    description = "Membaca file APK dari kartu SD / penyimpanan eksternal (Android 12 ke bawah).",
                    isGranted = checkPerm(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    isRuntime = true,
                    category = "Penyimpanan Legacy"
                )
            )
        }

        list.add(
            PermissionItem(
                name = "Internet",
                permissionString = android.Manifest.permission.INTERNET,
                description = "Izin normal sistem Android untuk konektivitas jika diperlukan.",
                isGranted = true,
                isRuntime = false,
                category = "Jaringan"
            )
        )

        _permissionsList.value = list
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
