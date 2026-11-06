package com.example.core.parser

import com.example.core.model.DexAnalysisResult
import java.nio.ByteBuffer
import java.nio.ByteOrder

object DexHeaderParser {

    fun isDex(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        val magic = String(bytes.take(4).toByteArray(), Charsets.US_ASCII)
        return magic == "dex\n"
    }

    fun parse(dexName: String, bytes: ByteArray): DexAnalysisResult {
        if (!isDex(bytes) || bytes.size < 112) {
            return DexAnalysisResult(
                dexFileName = dexName,
                magic = "Invalid / Not DEX",
                checksum = 0,
                signatureHex = "",
                fileSize = bytes.size.toLong(),
                headerSize = 0,
                stringIdsSize = 0,
                typeIdsSize = 0,
                protoIdsSize = 0,
                fieldIdsSize = 0,
                methodIdsSize = 0,
                classDefsSize = 0
            )
        }

        try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            val magicBytes = ByteArray(8)
            buffer.get(magicBytes)
            val magicStr = String(magicBytes, Charsets.US_ASCII).replace("\n", "\\n").replace("\u0000", "\\0")

            val checksum = buffer.int.toLong() and 0xFFFFFFFFL

            val signature = ByteArray(20)
            buffer.get(signature)
            val signatureHex = signature.joinToString("") { "%02x".format(it) }

            val fileSize = buffer.int.toLong() and 0xFFFFFFFFL
            val headerSize = buffer.int.toLong() and 0xFFFFFFFFL
            val endianTag = buffer.int

            val linkSize = buffer.int
            val linkOff = buffer.int
            val mapOff = buffer.int

            val stringIdsSize = buffer.int
            val stringIdsOff = buffer.int

            val typeIdsSize = buffer.int
            val typeIdsOff = buffer.int

            val protoIdsSize = buffer.int
            val protoIdsOff = buffer.int

            val fieldIdsSize = buffer.int
            val fieldIdsOff = buffer.int

            val methodIdsSize = buffer.int
            val methodIdsOff = buffer.int

            val classDefsSize = buffer.int
            val classDefsOff = buffer.int

            val dataSize = buffer.int
            val dataOff = buffer.int

            // Read string table sample
            val stringOffsets = mutableListOf<Int>()
            if (stringIdsOff in 0 until bytes.size && stringIdsSize > 0) {
                buffer.position(stringIdsOff)
                val readLimit = minOf(stringIdsSize, 2000)
                for (i in 0 until readLimit) {
                    if (buffer.remaining() >= 4) {
                        stringOffsets.add(buffer.int)
                    }
                }
            }

            // Decode some strings
            val sampleStrings = mutableListOf<String>()
            for (off in stringOffsets.take(500)) {
                if (off in 0 until bytes.size) {
                    buffer.position(off)
                    // MUTF-8 length (ULEB128)
                    readUleb128(buffer)
                    val sb = StringBuilder()
                    while (buffer.hasRemaining()) {
                        val b = buffer.get()
                        if (b.toInt() == 0) break
                        sb.append(b.toInt().toChar())
                        if (sb.length > 200) break
                    }
                    val str = sb.toString()
                    if (str.isNotBlank()) {
                        sampleStrings.add(str)
                    }
                }
            }

            // Type IDs sample (indices into string table)
            val sampleClasses = sampleStrings
                .filter { it.startsWith("L") && it.endsWith(";") && it.contains("/") }
                .map { it.removePrefix("L").removeSuffix(";").replace('/', '.') }
                .distinct()
                .take(100)

            val samplePackages = sampleClasses
                .mapNotNull {
                    val lastDot = it.lastIndexOf('.')
                    if (lastDot > 0) it.substring(0, lastDot) else null
                }
                .distinct()
                .take(30)

            val sampleMethods = sampleStrings
                .filter { (it.startsWith("<init>") || it.startsWith("<clinit>") || it.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]{2,}$"))) && !it.contains("/") }
                .distinct()
                .take(50)

            return DexAnalysisResult(
                dexFileName = dexName,
                magic = magicStr,
                checksum = checksum,
                signatureHex = signatureHex,
                fileSize = fileSize,
                headerSize = headerSize,
                stringIdsSize = stringIdsSize,
                typeIdsSize = typeIdsSize,
                protoIdsSize = protoIdsSize,
                fieldIdsSize = fieldIdsSize,
                methodIdsSize = methodIdsSize,
                classDefsSize = classDefsSize,
                sampleClasses = sampleClasses,
                sampleMethods = sampleMethods,
                samplePackages = samplePackages
            )
        } catch (e: Exception) {
            return DexAnalysisResult(
                dexFileName = dexName,
                magic = "dex",
                checksum = 0,
                signatureHex = "",
                fileSize = bytes.size.toLong(),
                headerSize = 112,
                stringIdsSize = 0,
                typeIdsSize = 0,
                protoIdsSize = 0,
                fieldIdsSize = 0,
                methodIdsSize = 0,
                classDefsSize = 0,
                sampleClasses = listOf("Error parsing DEX: ${e.message}")
            )
        }
    }

    private fun readUleb128(buffer: ByteBuffer): Int {
        var result = 0
        var shift = 0
        while (buffer.hasRemaining()) {
            val byte = buffer.get().toInt()
            result = result or ((byte and 0x7F) shl shift)
            if ((byte and 0x80) == 0) break
            shift += 7
        }
        return result
    }
}
