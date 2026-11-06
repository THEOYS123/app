package com.example.core.parser

import java.nio.ByteBuffer
import java.nio.ByteOrder

object AndroidBinaryXmlParser {

    private const val RES_XML_TYPE = 0x00080003
    private const val CHUNK_STRING_POOL = 0x001C0001
    private const val CHUNK_RES_MAP = 0x00080180
    private const val CHUNK_START_NAMESPACE = 0x00100100
    private const val CHUNK_END_NAMESPACE = 0x00100101
    private const val CHUNK_START_TAG = 0x00100102
    private const val CHUNK_END_TAG = 0x00100103
    private const val CHUNK_TEXT = 0x00100104

    fun isBinaryXml(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buffer.int
        return magic == RES_XML_TYPE
    }

    fun parse(bytes: ByteArray): String {
        if (!isBinaryXml(bytes)) {
            // Already plaintext or not binary XML
            return String(bytes, Charsets.UTF_8)
        }

        try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            buffer.int // magic RES_XML_TYPE
            val fileSize = buffer.int

            val strings = mutableListOf<String>()
            val namespaces = mutableMapOf<String, String>() // prefix -> uri
            val out = StringBuilder()
            out.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")

            var indent = 0

            while (buffer.hasRemaining()) {
                val chunkStart = buffer.position()
                if (chunkStart + 8 > bytes.size) break

                val chunkType = buffer.int
                val chunkSize = buffer.int

                if (chunkSize <= 0 || chunkStart + chunkSize > bytes.size) {
                    break
                }

                when (chunkType) {
                    CHUNK_STRING_POOL -> {
                        parseStringPool(buffer, chunkStart, chunkSize, strings)
                        buffer.position(chunkStart + chunkSize)
                    }
                    CHUNK_RES_MAP -> {
                        // Resource IDs mapping, skip
                        buffer.position(chunkStart + chunkSize)
                    }
                    CHUNK_START_NAMESPACE -> {
                        buffer.int // lineNumber
                        buffer.int // comment
                        val prefixIdx = buffer.int
                        val uriIdx = buffer.int
                        val prefix = strings.getOrNull(prefixIdx).orEmpty()
                        val uri = strings.getOrNull(uriIdx).orEmpty()
                        if (prefix.isNotEmpty() && uri.isNotEmpty()) {
                            namespaces[prefix] = uri
                        }
                        buffer.position(chunkStart + chunkSize)
                    }
                    CHUNK_END_NAMESPACE -> {
                        buffer.position(chunkStart + chunkSize)
                    }
                    CHUNK_START_TAG -> {
                        buffer.int // lineNumber
                        buffer.int // comment
                        val nsIdx = buffer.int
                        val nameIdx = buffer.int
                        buffer.short // attrStart
                        buffer.short // attrSize
                        val attrCount = buffer.short.toInt() and 0xFFFF
                        buffer.short // idIndex
                        buffer.short // classIndex
                        buffer.short // styleIndex

                        val tagName = strings.getOrNull(nameIdx) ?: "unknown_tag"

                        out.append("  ".repeat(indent))
                        out.append("<").append(tagName)

                        // If root tag (indent == 0), inject namespaces
                        if (indent == 0 && namespaces.isNotEmpty()) {
                            for ((prefix, uri) in namespaces) {
                                out.append("\n").append("  ".repeat(indent + 2))
                                out.append("xmlns:").append(prefix).append("=\"").append(uri).append("\"")
                            }
                        }

                        // Read attributes
                        for (i in 0 until attrCount) {
                            val attrNsIdx = buffer.int
                            val attrNameIdx = buffer.int
                            val rawValueIdx = buffer.int
                            val typedValueSize = buffer.short.toInt() and 0xFFFF
                            buffer.get() // res0
                            val dataType = buffer.get().toInt() and 0xFF
                            val data = buffer.int

                            val attrNs = strings.getOrNull(attrNsIdx)
                            val attrName = strings.getOrNull(attrNameIdx) ?: "attr$i"
                            val prefix = if (!attrNs.isNullOrEmpty()) {
                                namespaces.entries.firstOrNull { it.value == attrNs }?.key?.let { "$it:" } ?: "android:"
                            } else ""

                            val attrValue = if (rawValueIdx != -1 && rawValueIdx < strings.size) {
                                strings[rawValueIdx]
                            } else {
                                formatTypedValue(dataType, data)
                            }

                            out.append("\n").append("  ".repeat(indent + 1))
                            out.append(prefix).append(attrName).append("=\"").append(escapeXml(attrValue)).append("\"")
                        }

                        out.append(">\n")
                        indent++
                        buffer.position(chunkStart + chunkSize)
                    }
                    CHUNK_END_TAG -> {
                        buffer.int // lineNumber
                        buffer.int // comment
                        val nsIdx = buffer.int
                        val nameIdx = buffer.int
                        val tagName = strings.getOrNull(nameIdx) ?: "unknown_tag"
                        indent = maxOf(0, indent - 1)
                        out.append("  ".repeat(indent))
                        out.append("</").append(tagName).append(">\n")
                        buffer.position(chunkStart + chunkSize)
                    }
                    CHUNK_TEXT -> {
                        buffer.int // lineNumber
                        buffer.int // comment
                        val textIdx = buffer.int
                        buffer.int // unknown
                        buffer.int // unknown
                        val text = strings.getOrNull(textIdx)
                        if (!text.isNullOrBlank()) {
                            out.append("  ".repeat(indent))
                            out.append(escapeXml(text)).append("\n")
                        }
                        buffer.position(chunkStart + chunkSize)
                    }
                    else -> {
                        buffer.position(chunkStart + chunkSize)
                    }
                }
            }

            return out.toString()
        } catch (e: Exception) {
            return "<!-- Failed to fully decode Android Binary XML: ${e.message} -->\n" +
                    String(bytes.take(2048).toByteArray(), Charsets.ISO_8859_1)
        }
    }

    private fun parseStringPool(
        buffer: ByteBuffer,
        chunkStart: Int,
        chunkSize: Int,
        strings: MutableList<String>
    ) {
        val stringCount = buffer.int
        val styleCount = buffer.int
        val flags = buffer.int
        val stringsStart = buffer.int
        val stylesStart = buffer.int

        val isUtf8 = (flags and (1 shl 8)) != 0

        val offsets = IntArray(stringCount)
        for (i in 0 until stringCount) {
            offsets[i] = buffer.int
        }

        val stringsAbsoluteStart = chunkStart + stringsStart

        for (i in 0 until stringCount) {
            val strPos = stringsAbsoluteStart + offsets[i]
            if (strPos >= buffer.capacity()) {
                strings.add("")
                continue
            }
            buffer.position(strPos)

            if (isUtf8) {
                // UTF-8 string format: u8/u16 utf16_len, u8/u16 utf8_len, bytes, 0x00
                var u16Len = buffer.get().toInt() and 0xFF
                if ((u16Len and 0x80) != 0) {
                    u16Len = ((u16Len and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
                }
                var u8Len = buffer.get().toInt() and 0xFF
                if ((u8Len and 0x80) != 0) {
                    u8Len = ((u8Len and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
                }
                val strBytes = ByteArray(u8Len)
                buffer.get(strBytes)
                strings.add(String(strBytes, Charsets.UTF_8))
            } else {
                // UTF-16 string format: u16 char_count, chars, 0x0000
                var len = buffer.short.toInt() and 0xFFFF
                if ((len and 0x8000) != 0) {
                    len = ((len and 0x7FFF) shl 16) or (buffer.short.toInt() and 0xFFFF)
                }
                val chars = CharArray(len)
                for (c in 0 until len) {
                    chars[c] = buffer.short.toInt().toChar()
                }
                strings.add(String(chars))
            }
        }
    }

    private fun formatTypedValue(dataType: Int, data: Int): String {
        return when (dataType) {
            0x00 -> "" // TYPE_NULL
            0x01 -> "@ref/0x%08X".format(data) // TYPE_REFERENCE
            0x02 -> "?attr/0x%08X".format(data) // TYPE_ATTRIBUTE
            0x03 -> "string#$data" // TYPE_STRING
            0x04 -> java.lang.Float.intBitsToFloat(data).toString() // TYPE_FLOAT
            0x05 -> "${data shr 8}${getDimensionUnit(data and 0xFF)}" // TYPE_DIMENSION
            0x06 -> "${data shr 8}${getFractionUnit(data and 0xFF)}" // TYPE_FRACTION
            0x10 -> data.toString() // TYPE_INT_DEC
            0x11 -> "0x%08X".format(data) // TYPE_INT_HEX
            0x12 -> if (data != 0) "true" else "false" // TYPE_INT_BOOLEAN
            in 0x1C..0x1F -> "#%08X".format(data) // TYPE_INT_COLOR
            else -> "0x%08X".format(data)
        }
    }

    private fun getDimensionUnit(type: Int): String {
        return when (type) {
            0 -> "px"
            1 -> "dp"
            2 -> "sp"
            3 -> "pt"
            4 -> "in"
            5 -> "mm"
            else -> ""
        }
    }

    private fun getFractionUnit(type: Int): String {
        return when (type) {
            0 -> "%"
            1 -> "%p"
            else -> ""
        }
    }

    private fun escapeXml(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
