package io.honeycomb.opentelemetry.android.example

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.honeycomb.opentelemetry.android.Honeycomb
import io.honeycomb.opentelemetry.android.HoneycombOptions
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.semconv.ServiceAttributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 *
 * These tests are intended to exercise SKD initialization.
 */
@RunWith(AndroidJUnit4::class)
class HoneycombResourceAttributesTest {
    @Test
    fun resourceAttributes_areSetCorrectly() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val app = appContext.applicationContext as android.app.Application

        // Configure Honeycomb with test options
        val options =
            HoneycombOptions
                .builder(app)
                .setApiKey("test-key")
                .setServiceName("test-service")
                .build()

        Honeycomb.configure(app, options)

        // Get the resource and verify attributes
        val resource = Honeycomb.resource
        assertNotNull("Resource should not be null", resource)

        val attributes = resource.attributes

        // Verify service name
        val serviceName = attributes.get(ServiceAttributes.SERVICE_NAME)
        assertNotNull("Service name should be set", serviceName)
        assertEquals("test-service", serviceName)

        // Verify RUM SDK version is set
        val rumSdkVersion = attributes.get(AttributeKey.stringKey("rum.sdk.version"))
        assertNotNull("RUM SDK version should be set", rumSdkVersion)

        // Verify device attributes
        val deviceModelName = attributes.get(AttributeKey.stringKey("device.model.name"))
        assertNotNull("Device model name should be set", deviceModelName)
        assertEquals(Build.MODEL, deviceModelName)

        val deviceModelId = attributes.get(AttributeKey.stringKey("device.model.identifier"))
        assertNotNull("Device model identifier should be set", deviceModelId)
        assertEquals(Build.MODEL, deviceModelId)

        val deviceManufacturer = attributes.get(AttributeKey.stringKey("device.manufacturer"))
        assertNotNull("Device manufacturer should be set", deviceManufacturer)
        assertEquals(Build.MANUFACTURER, deviceManufacturer)

        // Verify OS attributes
        val osName = attributes.get(AttributeKey.stringKey("os.name"))
        assertNotNull("OS name should be set", osName)
        assertEquals("Android", osName)

        val osType = attributes.get(AttributeKey.stringKey("os.type"))
        assertNotNull("OS type should be set", osType)
        assertEquals("linux", osType)

        val osVersion = attributes.get(AttributeKey.stringKey("os.version"))
        assertNotNull("OS version should be set", osVersion)
        assertEquals(Build.VERSION.RELEASE, osVersion)

        val osDescription = attributes.get(AttributeKey.stringKey("os.description"))
        assertNotNull("OS description should be set", osDescription)
        assertTrue("OS description should contain Android Version", osDescription!!.contains("Android Version"))
        assertTrue("OS description should contain build info", osDescription.contains("Build"))
        assertTrue("OS description should contain API level", osDescription.contains("API level"))
    }
}
