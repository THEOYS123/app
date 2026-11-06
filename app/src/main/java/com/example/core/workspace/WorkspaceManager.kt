package com.example.core.workspace

import android.content.Context
import android.net.Uri
import com.example.core.archive.ApkArchiveManager
import com.example.core.logging.AppLogger
import com.example.core.model.WorkspaceItem
import com.example.core.parser.AndroidBinaryXmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object WorkspaceManager {

    private const val TAG = "WorkspaceManager"

    private fun getWorkspaceDir(context: Context, apkName: String): File {
        val safeName = apkName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val dir = File(context.filesDir, "workspace/$safeName")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun getWorkspaceItems(context: Context, apkName: String): List<WorkspaceItem> =
        withContext(Dispatchers.IO) {
            val root = getWorkspaceDir(context, apkName)
            val items = mutableListOf<WorkspaceItem>()

            fun scan(current: File) {
                current.listFiles()?.forEach { f ->
                    if (f.isDirectory) {
                        scan(f)
                    } else {
                        val relPath = f.relativeTo(root).path.replace('\\', '/')
                        val ext = f.extension.lowercase()
                        items.add(
                            WorkspaceItem(
                                relativePath = relPath,
                                sizeBytes = f.length(),
                                lastModified = f.lastModified(),
                                isModifiedFromApk = true,
                                extension = ext
                            )
                        )
                    }
                }
            }

            scan(root)
            items.sortedBy { it.relativePath }
        }

    suspend fun extractEntryToWorkspace(
        context: Context,
        apkName: String,
        entryPath: String,
        decodeBinaryXml: Boolean = true
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val bytes = ApkArchiveManager.readEntryBytes(entryPath).getOrThrow()
            val workspaceDir = getWorkspaceDir(context, apkName)
            val targetFile = File(workspaceDir, entryPath)
            targetFile.parentFile?.mkdirs()

            if (decodeBinaryXml && entryPath.endsWith(".xml", ignoreCase = true) && AndroidBinaryXmlParser.isBinaryXml(bytes)) {
                val decoded = AndroidBinaryXmlParser.parse(bytes)
                targetFile.writeText(decoded, Charsets.UTF_8)
            } else {
                targetFile.writeBytes(bytes)
            }

            AppLogger.i(TAG, "Extracted '$entryPath' to workspace: ${targetFile.absolutePath}")
            Result.success(targetFile)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to extract '$entryPath' to workspace", e)
            Result.failure(e)
        }
    }

    suspend fun saveFileContent(
        context: Context,
        apkName: String,
        relativePath: String,
        content: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val workspaceDir = getWorkspaceDir(context, apkName)
            val targetFile = File(workspaceDir, relativePath)
            targetFile.parentFile?.mkdirs()
            targetFile.writeText(content, Charsets.UTF_8)
            AppLogger.i(TAG, "Saved edited file '$relativePath' in workspace (${content.length} chars)")
            Result.success(targetFile)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save file '$relativePath'", e)
            Result.failure(e)
        }
    }

    suspend fun getWorkspaceFileText(
        context: Context,
        apkName: String,
        relativePath: String
    ): String? = withContext(Dispatchers.IO) {
        val file = File(getWorkspaceDir(context, apkName), relativePath)
        if (file.exists() && file.canRead()) {
            file.readText(Charsets.UTF_8)
        } else {
            null
        }
    }

    suspend fun deleteWorkspaceItem(
        context: Context,
        apkName: String,
        relativePath: String
    ): Boolean = withContext(Dispatchers.IO) {
        val file = File(getWorkspaceDir(context, apkName), relativePath)
        val deleted = if (file.exists()) file.delete() else false
        if (deleted) {
            AppLogger.i(TAG, "Deleted workspace item '$relativePath'")
        }
        deleted
    }

    suspend fun clearWorkspace(context: Context, apkName: String): Boolean =
        withContext(Dispatchers.IO) {
            val dir = getWorkspaceDir(context, apkName)
            val deleted = dir.deleteRecursively()
            AppLogger.i(TAG, "Cleared workspace for '$apkName'")
            deleted
        }

    suspend fun exportWorkspaceAsZip(
        context: Context,
        apkName: String,
        outputStream: OutputStream
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = getWorkspaceDir(context, apkName)
            val files = mutableListOf<File>()

            fun collect(f: File) {
                if (f.isDirectory) {
                    f.listFiles()?.forEach { collect(it) }
                } else {
                    files.add(f)
                }
            }
            collect(root)

            ZipOutputStream(outputStream).use { zos ->
                for (file in files) {
                    val relPath = file.relativeTo(root).path.replace('\\', '/')
                    val entry = ZipEntry(relPath)
                    zos.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }

            AppLogger.i(TAG, "Exported workspace for '$apkName' (${files.size} files) to ZIP archive")
            Result.success(files.size)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to export workspace as ZIP", e)
            Result.failure(e)
        }
    }

    suspend fun exportSingleFile(
        context: Context,
        apkName: String,
        relativePath: String,
        outputStream: OutputStream
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val workspaceFile = File(getWorkspaceDir(context, apkName), relativePath)
            var bytesWritten = 0L

            if (workspaceFile.exists()) {
                workspaceFile.inputStream().use { input ->
                    bytesWritten = input.copyTo(outputStream)
                }
            } else {
                // Read from original APK
                val bytes = ApkArchiveManager.readEntryBytes(relativePath).getOrThrow()
                outputStream.write(bytes)
                bytesWritten = bytes.size.toLong()
            }
            AppLogger.i(TAG, "Exported file '$relativePath' ($bytesWritten bytes)")
            Result.success(bytesWritten)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to export single file '$relativePath'", e)
            Result.failure(e)
        }
    }
}
