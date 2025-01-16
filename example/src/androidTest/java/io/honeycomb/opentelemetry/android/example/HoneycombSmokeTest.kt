package io.honeycomb.opentelemetry.android.example

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
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

// How long to wait for the UI to update between interactions.
val UI_WAIT_TIMEOUT = 5.seconds

// This is a special directory that will be saved to the build directory after the test finishes.
const val ADDITIONAL_TEST_OUTPUT_DIRECTORY = "/sdcard/Android/media/io.honeycomb.opentelemetry.android.example/additional_test_output"

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

        rule.waitUntil(UI_WAIT_TIMEOUT.toLong(DurationUnit.MILLISECONDS)) {
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
        // Let it do slow renders for two seconds to capture some traces.
        Thread.sleep(2000)
        rule.onNodeWithText("Normal").performClick()
    }

    @Test
    fun frozenRendersDetection_works() {
        rule.onNodeWithText("UI").performClick()
        rule.onNodeWithText("Frozen").performClick()
        // Let it do frozen renders for two seconds to capture some traces.
        Thread.sleep(2000)
        rule.onNodeWithText("Normal").performClick()
    }

    private fun buttonSelector(text: String): BySelector {
        return By.text(text.toUpperCase(Locale.current)).clazz("android.widget.Button")
    }

    /**
     * Attempts to get the button with the given text, or else throws.
     * This function will retry multiple times, and deal with any ANR dialogs in the way.
     */
    private fun getButton(
        device: UiDevice,
        text: String,
        retries: Int = 3,
    ): UiObject2 {
        // Attempt to get the button.
        val button: UiObject2? =
            device.wait(
                Until.findObject(buttonSelector(text)),
                UI_WAIT_TIMEOUT.toLong(DurationUnit.MILLISECONDS),
            )
        if (button != null) {
            return button
        }

        if (retries <= 0) {
            // We failed, so attempt to take a screenshot and throw an exception.
            val screenshotPath = File(ADDITIONAL_TEST_OUTPUT_DIRECTORY, "getButton_failure.png")
            if (device.takeScreenshot(screenshotPath)) {
                throw RuntimeException("Example Button missing. See screenshot at $screenshotPath")
            } else {
                throw RuntimeException("Example Button missing. Unable to take screenshot.")
            }
        }

        // If the Activity doesn't load in a few seconds, then it probably actually has loaded,
        // but cannot be found because of something like an ANR dialog blocking the app.
        // If it _is_ an ANR dialog, then attempt to close it.
        val waitButton: UiObject2? =
            device.wait(
                Until.findObject(By.text("Wait")),
                UI_WAIT_TIMEOUT.toLong(DurationUnit.MILLISECONDS),
            )
        waitButton?.click()

        return getButton(device, text, retries - 1)
    }

    @Test
    fun touchInstrumentation_works() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        rule.onNodeWithText("UI").performClick()
        rule.onNodeWithText("Start XML UI").performClick()

        val exampleButton = getButton(device, "Example Button")
        exampleButton.click()

        val backButton = getButton(device, "Back")
        backButton.clickAndWait(
            Until.newWindow(),
            UI_WAIT_TIMEOUT.toLong(DurationUnit.MILLISECONDS),
        )
    }

    @Test
    fun renderInstrumentation_works() {
        rule.onNodeWithText("Render").performClick()
        rule.onNodeWithTag("slow_render_switch").performClick()

        rule.waitUntil(UI_WAIT_TIMEOUT.toLong(DurationUnit.MILLISECONDS)) {
            rule.onAllNodesWithText("slow text", true).assertCountEquals(5)
            true
        }

        rule.onNodeWithText("Core").performClick()
    }
}
