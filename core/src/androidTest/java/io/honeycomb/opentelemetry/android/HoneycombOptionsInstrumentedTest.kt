package io.honeycomb.opentelemetry.android

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class HoneycombOptionsInstrumentedTest {
    @Test
    fun builder_usesContext() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.honeycomb.opentelemetry.android.test", context.packageName)

        val options = HoneycombOptions.builder(context).build()
        assertEquals("key", options.tracesApiKey)
    }

    @Test
    fun runtimeVersion_isAutomaticallySet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val options = HoneycombOptions.builder(context).build()
        assertEquals(
            mapOf(
                "service.name" to "unknown_service",
                "honeycomb.distro.version" to "0.0.1-experimental2",
                "honeycomb.distro.runtime_version" to Build.VERSION.RELEASE,
            ),
            options.resourceAttributes,
        )
    }

    @Test
    fun source_getString() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = HoneycombOptionsResourceSource(context)
        assertEquals("some string", source.getString("STRING"))
    }

    @Test
    fun source_getNullForEmptyString() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = HoneycombOptionsResourceSource(context)
        assertNull(source.getString("EMPTY_STRING"))
    }

    @Test
    fun source_getNullForBlankString() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = HoneycombOptionsResourceSource(context)
        assertNull(source.getString("BLANK_STRING"))
    }

    @Test
    fun source_getInt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = HoneycombOptionsResourceSource(context)
        assertEquals(5000, source.getInt("NUMBER"))
    }

    @Test
    fun source_getDuration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = HoneycombOptionsResourceSource(context)
        assertEquals(5.seconds, source.getDuration("NUMBER"))
    }

    @Test
    fun source_getBoolean() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = HoneycombOptionsResourceSource(context)
        assertEquals(true, source.getBoolean("BOOL"))
    }
}
