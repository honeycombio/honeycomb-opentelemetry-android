package io.honeycomb.passthrough.harness

import android.app.Application
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import io.honeycomb.opentelemetry.android.Honeycomb
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

class SmokeTestException(
    message: String?,
) : Exception(message)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HarnessTest {
    private fun tap(
        activity: HarnessActivity,
        x: Float,
        y: Float,
    ) {
        val props =
            arrayOf(
                MotionEvent.PointerProperties().apply {
                    id = 0
                    toolType = MotionEvent.TOOL_TYPE_FINGER
                },
            )

        fun event(
            action: Int,
            downTime: Long,
            time: Long,
        ): MotionEvent {
            val coords =
                arrayOf(
                    MotionEvent.PointerCoords().apply {
                        this.x = x
                        this.y = y
                    },
                )
            return MotionEvent.obtain(downTime, time, action, 1, props, coords, 0, 0, 1f, 1f, 0, 0, 0x1002, 0)
        }
        val downTime = SystemClock.uptimeMillis()
        activity.window.callback.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, downTime, downTime))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(60))
        activity.window.callback.dispatchTouchEvent(event(MotionEvent.ACTION_UP, downTime, SystemClock.uptimeMillis()))
        // Let GestureDetector confirm a single tap (it waits out the double-tap timeout).
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
    }

    private fun scenario(
        setup: String,
        app: Application,
        rum: OpenTelemetryRum,
    ) {
        // Activity + fragment lifecycle.
        val controller =
            Robolectric
                .buildActivity(HarnessActivity::class.java)
                .create()
                .start()
                .resume()
                .visible()
        val activity = controller.get()
        val root = activity.findViewById<View>(android.R.id.content)
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 1080, 1920)
        activity.supportFragmentManager
            .beginTransaction()
            .add(HarnessFragment(), "harness-fragment")
            .commitNow()
        shadowOf(Looper.getMainLooper()).idle()

        // A tap on the button.
        tap(activity, 100f, 100f)

        // Manual telemetry: span with baggage, log, metric.
        val otel = rum.openTelemetry
        Baggage.builder().put("baggage-key", "baggage-value").build().makeCurrent().use {
            otel
                .getTracer("harness.manual")
                .spanBuilder("manual-span")
                .startSpan()
                .end()
            otel
                .logsBridge
                .get("harness.manual")
                .logRecordBuilder()
                .setBody("manual log")
                .emit()
        }
        otel
            .getMeter("harness.manual")
            .counterBuilder("harness.counter")
            .build()
            .add(1)

        // Handled exception through the SDK's own API (only the current SDK has one).
        if (setup == "current") {
            Honeycomb.logException(
                rum,
                RuntimeException("This exception was intentional."),
                Attributes.builder().put("user.name", "bufo").build(),
                Thread.currentThread(),
            )
        }

        // Unhandled exception: invoke the handler the crash instrumentation installed. The
        // previous handler (installed before the SDK started) swallows it.
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(
            Thread.currentThread(),
            SmokeTestException("This crash was intentional."),
        )

        // ANR: the main looper is paused, so pings posted by the ANR watcher are never handled
        // while this thread sleeps.
        if (System.getProperty("harness.anr", "true") == "true") {
            // The watcher polls every ~2s and reports after 5 missed polls.
            Thread.sleep(14000)
        }

        controller.pause().stop().destroy()
        shadowOf(Looper.getMainLooper()).idle()

        val sdk = otel as OpenTelemetrySdk
        sdk.sdkTracerProvider.forceFlush().join(10, TimeUnit.SECONDS)
        sdk.sdkLoggerProvider.forceFlush().join(10, TimeUnit.SECONDS)
        sdk.sdkMeterProvider.forceFlush().join(10, TimeUnit.SECONDS)
    }

    @Test
    fun runScenario() {
        val setup = System.getProperty("harness.setup", "current")
        val out = File(System.getProperty("harness.out", "build/harness"))
        val app = ApplicationProvider.getApplicationContext<Application>()
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
        OtlpSink(File(out, "$setup.jsonl")).use { sink ->
            val rum = startSetup(setup, app, sink.endpoint)
            // Exporters are attached to the SDK from a background thread shortly after build().
            Thread.sleep(3000)
            scenario(setup, app, rum)
            Thread.sleep(1500)
        }
    }
}
