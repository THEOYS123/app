package com.example.core.model

import android.net.Uri

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val apkPath: String,
    val splitApkPaths: List<String> = emptyList(),
    val isSystemApp: Boolean = false,
    val sizeBytes: Long = 0L,
    val installTime: Long = 0L,
    val updateTime: Long = 0L,
    val targetSdkVersion: Int = 0,
    val minSdkVersion: Int = 0
)

data class ApkArchiveInfo(
    val name: String,
    val sourcePath: String,
    val sourceUri: Uri? = null,
    val packageName: String = "",
    val totalEntries: Int = 0,
    val totalUncompressedBytes: Long = 0L,
    val totalCompressedBytes: Long = 0L,
    val hasManifest: Boolean = false,
    val dexCount: Int = 0,
    val nativeAbis: List<String> = emptyList(),
    val sha256: String = "",
    val md5: String = ""
)

data class ApkFileEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val compressedSizeBytes: Long,
    val crc32: Long,
    val lastModifiedTime: Long,
    val extension: String,
    val mimeType: String,
    val isHidden: Boolean = false,
    val sha256: String = ""
)

enum class SearchStatus {
    IDLE,
    SCANNING,
    COMPLETED,
    NO_RESULTS,
    PARTIAL_SCAN,
    UNSUPPORTED_CONTENT,
    ACCESS_RESTRICTED,
    ERROR
}

data class SearchFilter(
    val query: String = "",
    val extension: String = "",
    val searchInContent: Boolean = false,
    val directoryPath: String = "",
    val minSizeBytes: Long? = null,
    val maxSizeBytes: Long? = null,
    val isRegex: Boolean = false,
    val isCaseSensitive: Boolean = false,
    val matchPrefix: Boolean = false,
    val matchSuffix: Boolean = false,
    val includeHidden: Boolean = true
)

data class SearchResultItem(
    val entry: ApkFileEntry,
    val matchType: String, // "NAME", "EXTENSION", "CONTENT", "DIRECTORY"
    val snippet: String? = null,
    val lineNumber: Int? = null,
    val lineContent: String? = null
)

data class SearchSummary(
    val totalScannedFiles: Int,
    val totalScannedFolders: Int,
    val matchedCount: Int,
    val durationMs: Long,
    val status: SearchStatus,
    val explanationReason: String? = null
)

enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

data class LogEntry(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val errorStack: String? = null,
    val durationMs: Long? = null
)

data class DexAnalysisResult(
    val dexFileName: String,
    val magic: String,
    val checksum: Long,
    val signatureHex: String,
    val fileSize: Long,
    val headerSize: Long,
    val stringIdsSize: Int,
    val typeIdsSize: Int,
    val protoIdsSize: Int,
    val fieldIdsSize: Int,
    val methodIdsSize: Int,
    val classDefsSize: Int,
    val sampleClasses: List<String> = emptyList(),
    val sampleMethods: List<String> = emptyList(),
    val samplePackages: List<String> = emptyList()
)

data class WorkspaceItem(
    val relativePath: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isModifiedFromApk: Boolean,
    val extension: String
)
