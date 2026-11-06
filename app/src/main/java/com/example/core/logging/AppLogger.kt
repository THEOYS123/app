package com.example.core.logging

import com.example.core.model.LogEntry
import com.example.core.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private const val MAX_LOGS = 1000

    init {
        log(LogLevel.INFO, "AppLogger", "Deep APK Inspector logging system initialized")
    }

    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        durationMs: Long? = null
    ) {
        val stackTrace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        }

        val entry = LogEntry(
            level = level,
            tag = tag,
            message = message,
            errorStack = stackTrace,
            durationMs = durationMs
        )

        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > MAX_LOGS) {
            current.removeAt(current.size - 1)
        }
        _logs.value = current
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String, durationMs: Long? = null) = log(LogLevel.INFO, tag, message, durationMs = durationMs)
    fun w(tag: String, message: String) = log(LogLevel.WARNING, tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)
    fun c(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.CRITICAL, tag, message, throwable)

    fun clear() {
        _logs.value = emptyList()
        i("AppLogger", "Logs cleared")
    }

    fun exportAsText(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return buildString {
            appendLine("=== DEEP APK INSPECTOR SYSTEM LOGS ===")
            appendLine("Exported: ${sdf.format(Date())}")
            appendLine("Total Entries: ${_logs.value.size}")
            appendLine("======================================\n")
            _logs.value.reversed().forEach { entry ->
                appendLine("[${sdf.format(Date(entry.timestamp))}] [${entry.level}] [${entry.tag}]: ${entry.message}")
                if (entry.durationMs != null) {
                    appendLine("  Duration: ${entry.durationMs} ms")
                }
                if (!entry.errorStack.isNullOrBlank()) {
                    appendLine("  Stack Trace:\n${entry.errorStack}")
                }
            }
        }
    }
}
