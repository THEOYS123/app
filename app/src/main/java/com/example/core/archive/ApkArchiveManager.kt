package com.example.core.archive

import android.content.Context
import android.net.Uri
import com.example.core.logging.AppLogger
import com.example.core.model.ApkArchiveInfo
import com.example.core.model.ApkFileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ApkArchiveManager {

    private const val TAG = "ApkArchiveManager"

    // Currently opened archive
    private var currentZipFile: ZipFile? = null
    private var currentApkFile: File? = null
    private var currentInfo: ApkArchiveInfo? = null
    private var allEntries: List<ApkFileEntry> = emptyList()

    fun getCurrentArchiveInfo(): ApkArchiveInfo? = currentInfo
    fun getAllEntries(): List<ApkFileEntry> = allEntries
    fun getCurrentApkFile(): File? = currentApkFile

    suspend fun openFromPath(context: Context, filePath: String, displayName: String = ""): Result<ApkArchiveInfo> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                closeCurrent()
                val file = File(filePath)
                if (!file.exists() || !file.canRead()) {
                    val err = "File does not exist or cannot be read: $filePath"
                    AppLogger.e(TAG, err)
                    return@withContext Result.failure(IllegalArgumentException(err))
                }

                currentApkFile = file
                val zip = ZipFile(file)
                currentZipFile = zip

                val name = if (displayName.isNotBlank()) displayName else file.name
                val entries = parseEntries(zip)
                allEntries = entries

                val (sha256, md5) = calculateArchiveHashes(file)

                val info = buildArchiveInfo(name, filePath, null, entries, sha256, md5)
                currentInfo = info

                AppLogger.i(
                    TAG,
                    "Opened APK '$name': ${entries.size} entries, ${info.totalUncompressedBytes / 1024} KB in ${System.currentTimeMillis() - startTime}ms",
                    System.currentTimeMillis() - startTime
                )

                Result.success(info)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to open APK from path $filePath", e)
                Result.failure(e)
            }
        }

    suspend fun openFromUri(context: Context, uri: Uri, displayName: String = "Selected APK"): Result<ApkArchiveInfo> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                closeCurrent()
                val tempFile = File(context.cacheDir, "inspect_target_${System.currentTimeMillis()}.apk")
                if (tempFile.exists()) tempFile.delete()

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: run {
                    val err = "Failed to open input stream from URI: $uri"
                    AppLogger.e(TAG, err)
                    return@withContext Result.failure(IllegalStateException(err))
                }

                currentApkFile = tempFile
                val zip = ZipFile(tempFile)
                currentZipFile = zip

                val entries = parseEntries(zip)
                allEntries = entries

                val (sha256, md5) = calculateArchiveHashes(tempFile)
                val info = buildArchiveInfo(displayName, tempFile.absolutePath, uri, entries, sha256, md5)
                currentInfo = info

                AppLogger.i(
                    TAG,
                    "Opened APK from URI '$displayName': ${entries.size} entries in ${System.currentTimeMillis() - startTime}ms",
                    System.currentTimeMillis() - startTime
                )

                Result.success(info)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to open APK from URI $uri", e)
                Result.failure(e)
            }
        }

    private fun parseEntries(zip: ZipFile): List<ApkFileEntry> {
        val list = mutableListOf<ApkFileEntry>()
        val zipEntries = zip.entries()
        while (zipEntries.hasMoreElements()) {
            val ze = zipEntries.nextElement()
            val path = ze.name
            val isDir = ze.isDirectory
            val cleanPath = path.trimEnd('/')
            val name = cleanPath.substringAfterLast('/')
            val ext = if (isDir) "" else name.substringAfterLast('.', "").lowercase()
            val mime = detectMimeType(ext)
            val isHidden = name.startsWith(".")

            list.add(
                ApkFileEntry(
                    path = cleanPath,
                    name = name,
                    isDirectory = isDir,
                    sizeBytes = if (ze.size >= 0) ze.size else 0L,
                    compressedSizeBytes = if (ze.compressedSize >= 0) ze.compressedSize else 0L,
                    crc32 = ze.crc,
                    lastModifiedTime = ze.time,
                    extension = ext,
                    mimeType = mime,
                    isHidden = isHidden
                )
            )
        }
        return list.sortedWith(compareBy({ !it.isDirectory }, { it.path.lowercase() }))
    }

    private fun buildArchiveInfo(
        name: String,
        path: String,
        uri: Uri?,
        entries: List<ApkFileEntry>,
        sha256: String,
        md5: String
    ): ApkArchiveInfo {
        var totalUncompressed = 0L
        var totalCompressed = 0L
        var hasManifest = false
        var dexCount = 0
        val abis = mutableSetOf<String>()

        for (e in entries) {
            totalUncompressed += e.sizeBytes
            totalCompressed += e.compressedSizeBytes
            if (e.name.equals("AndroidManifest.xml", ignoreCase = true)) {
                hasManifest = true
            }
            if (e.name.endsWith(".dex", ignoreCase = true)) {
                dexCount++
            }
            if (e.path.startsWith("lib/")) {
                val parts = e.path.split('/')
                if (parts.size >= 2) {
                    abis.add(parts[1])
                }
            }
        }

        return ApkArchiveInfo(
            name = name,
            sourcePath = path,
            sourceUri = uri,
            totalEntries = entries.size,
            totalUncompressedBytes = totalUncompressed,
            totalCompressedBytes = totalCompressed,
            hasManifest = hasManifest,
            dexCount = dexCount,
            nativeAbis = abis.sorted(),
            sha256 = sha256,
            md5 = md5
        )
    }

    /**
     * Get immediate children of a given directory path in the APK archive.
     * If directoryPath is "" or "/", returns top-level entries.
     */
    fun getDirectoryEntries(directoryPath: String): List<ApkFileEntry> {
        val normalized = directoryPath.trim('/').let { if (it.isEmpty()) "" else "$it/" }
        val depth = if (normalized.isEmpty()) 0 else normalized.count { it == '/' }

        val result = mutableMapOf<String, ApkFileEntry>()

        for (e in allEntries) {
            val entryPath = e.path
            if (normalized.isEmpty()) {
                val firstSlash = entryPath.indexOf('/')
                if (firstSlash == -1) {
                    // Top-level file
                    result[entryPath] = e
                } else {
                    // Top-level directory
                    val topDir = entryPath.substring(0, firstSlash)
                    if (!result.containsKey(topDir)) {
                        result[topDir] = ApkFileEntry(
                            path = topDir,
                            name = topDir,
                            isDirectory = true,
                            sizeBytes = 0L,
                            compressedSizeBytes = 0L,
                            crc32 = 0L,
                            lastModifiedTime = 0L,
                            extension = "",
                            mimeType = "directory",
                            isHidden = topDir.startsWith(".")
                        )
                    }
                }
            } else {
                if (entryPath.startsWith(normalized) && entryPath != normalized.trimEnd('/')) {
                    val sub = entryPath.removePrefix(normalized)
                    val slashIdx = sub.indexOf('/')
                    if (slashIdx == -1) {
                        // Direct child file or empty folder
                        result[sub] = e
                    } else {
                        val subDirName = sub.substring(0, slashIdx)
                        val subDirPath = normalized + subDirName
                        if (!result.containsKey(subDirName)) {
                            result[subDirName] = ApkFileEntry(
                                path = subDirPath,
                                name = subDirName,
                                isDirectory = true,
                                sizeBytes = 0L,
                                compressedSizeBytes = 0L,
                                crc32 = 0L,
                                lastModifiedTime = 0L,
                                extension = "",
                                mimeType = "directory",
                                isHidden = subDirName.startsWith(".")
                            )
                        }
                    }
                }
            }
        }

        return result.values.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    suspend fun readEntryBytes(path: String, maxBytes: Int = 10 * 1024 * 1024): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            try {
                val zip = currentZipFile ?: return@withContext Result.failure(IllegalStateException("No APK archive open"))
                var ze = zip.getEntry(path)
                if (ze == null) {
                    ze = zip.getEntry("$path/")
                }
                if (ze == null) {
                    // Try case-insensitive lookup
                    val match = allEntries.firstOrNull { it.path.equals(path, ignoreCase = true) }
                    if (match != null) {
                        ze = zip.getEntry(match.path)
                    }
                }

                if (ze == null) {
                    return@withContext Result.failure(NoSuchElementException("Entry not found in archive: $path"))
                }

                zip.getInputStream(ze).use { input ->
                    val size = if (ze.size > 0 && ze.size <= maxBytes) ze.size.toInt() else maxBytes
                    val buffer = ByteArray(minOf(size, maxBytes))
                    var totalRead = 0
                    while (totalRead < buffer.size) {
                        val read = input.read(buffer, totalRead, buffer.size - totalRead)
                        if (read == -1) break
                        totalRead += read
                    }
                    val result = if (totalRead == buffer.size) buffer else buffer.copyOf(totalRead)
                    Result.success(result)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error reading entry bytes for $path", e)
                Result.failure(e)
            }
        }

    fun getEntryInputStream(path: String): InputStream? {
        val zip = currentZipFile ?: return null
        val ze = zip.getEntry(path) ?: zip.getEntry("$path/") ?: return null
        return zip.getInputStream(ze)
    }

    suspend fun computeEntrySha256(path: String): String = withContext(Dispatchers.IO) {
        try {
            val bytes = readEntryBytes(path, maxBytes = 20 * 1024 * 1024).getOrNull() ?: return@withContext ""
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun calculateArchiveHashes(file: File): Pair<String, String> {
        return try {
            val sha256Md = MessageDigest.getInstance("SHA-256")
            val md5Md = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    sha256Md.update(buffer, 0, read)
                    md5Md.update(buffer, 0, read)
                }
            }
            val sha256 = sha256Md.digest().joinToString("") { "%02x".format(it) }
            val md5 = md5Md.digest().joinToString("") { "%02x".format(it) }
            Pair(sha256, md5)
        } catch (e: Exception) {
            Pair("", "")
        }
    }

    fun closeCurrent() {
        try {
            currentZipFile?.close()
        } catch (_: Exception) {}
        currentZipFile = null
        currentInfo = null
        allEntries = emptyList()
    }

    private fun detectMimeType(ext: String): String {
        return when (ext.lowercase()) {
            "xml" -> "text/xml"
            "json" -> "application/json"
            "txt", "log", "md" -> "text/plain"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "dex" -> "application/octet-stream (DEX)"
            "so" -> "application/octet-stream (ELF)"
            "arsc" -> "application/octet-stream (ARSC)"
            "properties", "cfg", "conf", "ini" -> "text/x-properties"
            "ttf", "otf", "woff", "woff2" -> "font/ttf"
            "mp3", "ogg", "wav" -> "audio/mpeg"
            "mp4", "mkv", "webm" -> "video/mp4"
            else -> "application/octet-stream"
        }
    }
}
