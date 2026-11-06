package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.logging.AppLogger
import com.example.core.model.LogLevel
import com.example.core.parser.AndroidBinaryXmlParser
import com.example.core.parser.DexHeaderParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("APK Inspector", appName)
    }

    @Test
    fun `test binary xml parser identifies plaintext xml`() {
        val xmlPlain = "<?xml version=\"1.0\" encoding=\"utf-8\"?><manifest></manifest>".toByteArray(Charsets.UTF_8)
        assertFalse(AndroidBinaryXmlParser.isBinaryXml(xmlPlain))
        val parsed = AndroidBinaryXmlParser.parse(xmlPlain)
        assertTrue(parsed.contains("<manifest>"))
    }

    @Test
    fun `test dex header parser handles invalid input gracefully`() {
        val dummyBytes = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)
        assertFalse(DexHeaderParser.isDex(dummyBytes))
        val result = DexHeaderParser.parse("dummy.dex", dummyBytes)
        assertNotNull(result)
        assertEquals(0L, result.checksum)
    }

    @Test
    fun `test app logger records entries and exports text`() {
        AppLogger.clear()
        AppLogger.i("TestTag", "Testing logger functionality")
        AppLogger.e("TestTag", "Simulated error log", RuntimeException("Sample error"))

        val exported = AppLogger.exportAsText()
        assertTrue(exported.contains("Testing logger functionality"))
        assertTrue(exported.contains("Simulated error log"))
        assertTrue(exported.contains("RuntimeException"))
    }
}
