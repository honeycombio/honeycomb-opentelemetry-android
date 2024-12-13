package io.honeycomb.opentelemetry.android.example

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Instrumented test, which will execute on an Android device.
 *
 * These tests are primarily intended to exercise demo functionality in the test app in order to
 * make it emit various telemetry signals. The signals will then be verified by the assertions in
 * smoke-tests/smoke-e2e.bats
 */
@RunWith(AndroidJUnit4::class)
class HoneycombSmokeTest {
    companion object {
        @AfterClass @JvmStatic
        fun flush() {
            val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ExampleApp
            app.flush()
        }
    }

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.honeycomb.opentelemetry.android.example", appContext.packageName)
    }

    @Test
    fun span_works() {
        rule.onNodeWithText("Send Span").performClick()
    }

    @Test
    fun metric_works() {
        rule.onNodeWithText("Send Metric").performClick()
    }

    @Test
    fun network_instrumentation_works() {
        rule.onNodeWithText("Network").performClick()
        rule.onNodeWithText("Make a Network Request").performClick()

        rule.waitUntil(5000) {
            rule.onNodeWithText("Network Request Succeeded", true).isDisplayed()
        }
    }

    @Test
    fun anrDetection_works() {
        rule.onNodeWithText("Become Unresponsive (ANR)").performClick()
    }

    @Test
    fun slowRendersDetection_works() {
        rule.onNodeWithText("UI").performClick()
        rule.onNodeWithText("Slow").performClick()
        Thread.sleep(1000)
        rule.onNodeWithText("Normal").performClick()
    }

    @Test
    fun frozenRendersDetection_works() {
        rule.onNodeWithText("UI").performClick()
        rule.onNodeWithText("Frozen").performClick()
        Thread.sleep(1000)
        rule.onNodeWithText("Normal").performClick()
    }

    @Test
    fun touchInstrumentation_works() {
        rule.onNodeWithText("UI").performClick()
        rule.onNodeWithText("Start XML UI").performClick()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        val exampleButton: UiObject2? =
            device.findObject(
                By.text("EXAMPLE BUTTON").clazz("android.widget.Button"),
            )
        exampleButton!!.click()

        val backButton: UiObject2? =
            device.findObject(
                By.text("BACK").clazz("android.widget.Button"),
            )
        backButton!!.clickAndWait(Until.newWindow(), 5000)
    }
}
