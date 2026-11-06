package com.example.core.search

import com.example.core.archive.ApkArchiveManager
import com.example.core.logging.AppLogger
import com.example.core.model.ApkFileEntry
import com.example.core.model.SearchFilter
import com.example.core.model.SearchResultItem
import com.example.core.model.SearchStatus
import com.example.core.model.SearchSummary
import com.example.core.parser.AndroidBinaryXmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object ApkSearchEngine {

    private const val TAG = "ApkSearchEngine"

    private val _searchHistory = MutableStateFlow<List<String>>(
        listOf("AndroidManifest.xml", ".dex", ".json", ".so", "assets", "res/values")
    )
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchProgress = MutableStateFlow(0f)
    val searchProgress: StateFlow<Float> = _searchProgress.asStateFlow()

    fun addHistory(query: String) {
        if (query.isBlank()) return
        val list = _searchHistory.value.toMutableList()
        list.remove(query)
        list.add(0, query)
        _searchHistory.value = list.take(15)
    }

    fun clearHistory() {
        _searchHistory.value = emptyList()
    }

    suspend fun executeSearch(filter: SearchFilter): Pair<List<SearchResultItem>, SearchSummary> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            _isSearching.value = true
            _searchProgress.value = 0f

            val allEntries = ApkArchiveManager.getAllEntries()
            val results = mutableListOf<SearchResultItem>()

            var totalExaminedFiles = 0
            var totalExaminedFolders = 0
            var filterRejectedByExtension = 0
            var filterRejectedBySize = 0
            var filterRejectedByDirectory = 0
            var unsupportedContentCount = 0

            try {
                if (filter.query.isNotBlank()) {
                    addHistory(filter.query.trim())
                }

                val query = filter.query.trim()
                val regex = if (filter.isRegex && query.isNotEmpty()) {
                    try {
                        Regex(query, if (filter.isCaseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE))
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Invalid Regex query: $query: ${e.message}")
                        null
                    }
                } else null

                val filterExt = filter.extension.trim().removePrefix(".").lowercase()
                val targetDir = filter.directoryPath.trim('/').let { if (it.isEmpty()) "" else "$it/" }

                val textExtensions = setOf("xml", "json", "txt", "html", "htm", "css", "js", "properties", "cfg", "ini", "md")

                for ((index, entry) in allEntries.withIndex()) {
                    if (index % 100 == 0 && allEntries.isNotEmpty()) {
                        _searchProgress.value = index.toFloat() / allEntries.size
                    }

                    if (entry.isDirectory) {
                        totalExaminedFolders++
                    } else {
                        totalExaminedFiles++
                    }

                    // Hidden filter
                    if (!filter.includeHidden && entry.isHidden) {
                        continue
                    }

                    // Directory filter
                    if (targetDir.isNotEmpty() && !entry.path.startsWith(targetDir)) {
                        filterRejectedByDirectory++
                        continue
                    }

                    // Extension filter
                    if (filterExt.isNotEmpty() && !entry.extension.equals(filterExt, ignoreCase = true)) {
                        filterRejectedByExtension++
                        continue
                    }

                    // Size filter
                    if (filter.minSizeBytes != null && entry.sizeBytes < filter.minSizeBytes) {
                        filterRejectedBySize++
                        continue
                    }
                    if (filter.maxSizeBytes != null && entry.sizeBytes > filter.maxSizeBytes) {
                        filterRejectedBySize++
                        continue
                    }

                    // Match logic
                    var matched = false
                    var matchType = "NAME"
                    var snippet: String? = null
                    var lineNum: Int? = null

                    if (query.isEmpty()) {
                        // Empty query means match everything passing filters (e.g. extension filter or dir filter)
                        matched = true
                        matchType = if (filterExt.isNotEmpty()) "EXTENSION" else "ALL"
                    } else if (regex != null) {
                        if (regex.containsMatchIn(entry.name) || regex.containsMatchIn(entry.path)) {
                            matched = true
                            matchType = "REGEX_NAME"
                            snippet = entry.path
                        }
                    } else {
                        val nameToTest = if (filter.isCaseSensitive) entry.name else entry.name.lowercase()
                        val pathToTest = if (filter.isCaseSensitive) entry.path else entry.path.lowercase()
                        val qToTest = if (filter.isCaseSensitive) query else query.lowercase()

                        if (filter.matchPrefix && nameToTest.startsWith(qToTest)) {
                            matched = true
                            matchType = "PREFIX"
                        } else if (filter.matchSuffix && nameToTest.endsWith(qToTest)) {
                            matched = true
                            matchType = "SUFFIX"
                        } else if (nameToTest.contains(qToTest) || pathToTest.contains(qToTest)) {
                            matched = true
                            matchType = if (nameToTest.contains(qToTest)) "NAME" else "PATH"
                        }
                    }

                    // Content Search if enabled and not already matched or wanting content snippet
                    if (!entry.isDirectory && filter.searchInContent && query.isNotEmpty()) {
                        if (textExtensions.contains(entry.extension) || entry.name.endsWith(".xml", ignoreCase = true)) {
                            // Check file size limit for content search (max 2MB to keep performance fast)
                            if (entry.sizeBytes in 1..2_000_000) {
                                val bytes = ApkArchiveManager.readEntryBytes(entry.path, 2_000_000).getOrNull()
                                if (bytes != null) {
                                    val content = if (entry.extension == "xml" && AndroidBinaryXmlParser.isBinaryXml(bytes)) {
                                        AndroidBinaryXmlParser.parse(bytes)
                                    } else {
                                        String(bytes, Charsets.UTF_8)
                                    }

                                    val lines = content.lines()
                                    val qToTest = if (filter.isCaseSensitive) query else query.lowercase()

                                    for ((lIdx, line) in lines.withIndex()) {
                                        val lineToTest = if (filter.isCaseSensitive) line else line.lowercase()
                                        if (lineToTest.contains(qToTest)) {
                                            matched = true
                                            matchType = "CONTENT"
                                            lineNum = lIdx + 1
                                            snippet = line.trim()
                                            break
                                        }
                                    }
                                }
                            }
                        } else {
                            unsupportedContentCount++
                        }
                    }

                    if (matched) {
                        results.add(
                            SearchResultItem(
                                entry = entry,
                                matchType = matchType,
                                snippet = snippet,
                                lineNumber = lineNum,
                                lineContent = snippet
                            )
                        )
                    }
                }

                val duration = System.currentTimeMillis() - startTime
                _searchProgress.value = 1f
                _isSearching.value = false

                val status: SearchStatus
                val explanation: String?

                if (results.isNotEmpty()) {
                    status = SearchStatus.COMPLETED
                    explanation = null
                } else {
                    status = SearchStatus.NO_RESULTS
                    explanation = buildExplanation(
                        query = query,
                        filterExt = filterExt,
                        totalExaminedFiles = totalExaminedFiles,
                        totalExaminedFolders = totalExaminedFolders,
                        filterRejectedByExtension = filterRejectedByExtension,
                        filterRejectedBySize = filterRejectedBySize,
                        filterRejectedByDirectory = filterRejectedByDirectory,
                        searchInContent = filter.searchInContent,
                        unsupportedContentCount = unsupportedContentCount
                    )
                }

                AppLogger.i(
                    TAG,
                    "Search for '$query': ${results.size} matches from $totalExaminedFiles files in ${duration}ms (Status: $status)",
                    duration
                )

                val summary = SearchSummary(
                    totalScannedFiles = totalExaminedFiles,
                    totalScannedFolders = totalExaminedFolders,
                    matchedCount = results.size,
                    durationMs = duration,
                    status = status,
                    explanationReason = explanation
                )

                Pair(results, summary)
            } catch (e: Exception) {
                _isSearching.value = false
                AppLogger.e(TAG, "Search failed", e)
                val summary = SearchSummary(
                    totalScannedFiles = totalExaminedFiles,
                    totalScannedFolders = totalExaminedFolders,
                    matchedCount = 0,
                    durationMs = System.currentTimeMillis() - startTime,
                    status = SearchStatus.ERROR,
                    explanationReason = "Proses pencarian terhenti karena error: ${e.message}"
                )
                Pair(emptyList(), summary)
            }
        }

    private fun buildExplanation(
        query: String,
        filterExt: String,
        totalExaminedFiles: Int,
        totalExaminedFolders: Int,
        filterRejectedByExtension: Int,
        filterRejectedBySize: Int,
        filterRejectedByDirectory: Int,
        searchInContent: Boolean,
        unsupportedContentCount: Int
    ): String {
        return buildString {
            if (totalExaminedFiles == 0 && totalExaminedFolders == 0) {
                append("Tidak ada file dalam arsip yang dapat dipindai. Pastikan APK telah berhasil dimuat terlebih dahulu.")
                return@buildString
            }

            append("Tidak ditemukan hasil yang cocok setelah memeriksa $totalExaminedFiles file dan $totalExaminedFolders folder.")
            append("\n\nAnalisis teknis penyebab tidak ditemukan:")

            if (filterExt.isNotEmpty() && filterRejectedByExtension > 0) {
                append("\n• Filter Ekstensi: Sebanyak $filterRejectedByExtension file diabaikan karena tidak memiliki ekstensi '.$filterExt'.")
            }

            if (filterRejectedBySize > 0) {
                append("\n• Filter Ukuran: Sebanyak $filterRejectedBySize file diabaikan karena berada di luar rentang ukuran yang Anda tentukan.")
            }

            if (filterRejectedByDirectory > 0) {
                append("\n• Filter Direktori: File di luar cakupan direktori yang dipilih diabaikan.")
            }

            if (searchInContent) {
                append("\n• Pencarian Isi: Pencarian teks di dalam file hanya mendukung format berbasis teks (XML, JSON, TXT, HTML, CSS, JS). Sebanyak $unsupportedContentCount file biner/media tidak dapat diperiksa isinya.")
            }

            append("\n• Pencocokan Nama: Tidak ada entri dalam arsip yang nama atau path-nya memuat kata kunci '$query'.")
        }
    }
}
