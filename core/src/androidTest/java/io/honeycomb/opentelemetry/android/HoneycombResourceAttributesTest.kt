package io.honeycomb.opentelemetry.android.example

import android.os.Build
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import io.honeycomb.opentelemetry.android.Honeycomb
import io.honeycomb.opentelemetry.android.HoneycombOptions
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.semconv.ServiceAttributes
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

// How long to wait for the UI to update between interactions.
val UI_WAIT_TIMEOUT = 5.seconds

// This is a special directory that will be saved to the build directory after the test finishes.
const val ADDITIONAL_TEST_OUTPUT_DIRECTORY =
    "/sdcard/Android/media/io.honeycomb.opentelemetry.android.example/additional_test_output"

/**
 * Instrumented test, which will execute on an Android device.
 *
 * These tests are primarily intended to exercise demo functionality in the test app in order to
 * make it emit various telemetry signals. The signals will then be verified by the assertions in
 * smoke-tests/smoke-e2e.bats
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
