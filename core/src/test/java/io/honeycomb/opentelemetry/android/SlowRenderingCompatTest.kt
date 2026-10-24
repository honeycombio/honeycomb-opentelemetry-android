package io.honeycomb.opentelemetry.android

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.data.LogRecordData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private class CapturingProcessor : LogRecordProcessor {
    val records = mutableListOf<LogRecordData>()

    override fun onEmit(
        context: io.opentelemetry.context.Context,
        logRecord: ReadWriteLogRecord,
    ) {
        records.add(logRecord.toLogRecordData())
    }
}

/**
 * Tests for the workaround for the upstream slow rendering instrumentation, which crashes on
 * Android 7 - 11 because it calls Duration.toSeconds(), which was added in Android 12.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SlowRenderingCompatTest {
    @Test
    fun replacesUpstreamOnlyWhereUpstreamCrashes() {
        val upstreamIsPresent = shouldReplaceUpstreamSlowRendering(30, emptyList())
        assertTrue("android-agent is on the test classpath", upstreamIsPresent)

        assertFalse(shouldReplaceUpstreamSlowRendering(23, emptyList()))
        assertTrue(shouldReplaceUpstreamSlowRendering(24, emptyList()))
        assertTrue(shouldReplaceUpstreamSlowRendering(30, emptyList()))
        assertFalse(shouldReplaceUpstreamSlowRendering(31, emptyList()))
        assertFalse(shouldReplaceUpstreamSlowRendering(30, listOf("slowrendering")))
    }

    @Test
    fun reportsJankEventsWithFractionalPeriodWhenActivityPauses() {
        val processor = CapturingProcessor()
        val logger =
            SdkLoggerProvider
                .builder()
                .addLogRecordProcessor(processor)
                .build()
                .get("test")
        val listener =
            SlowRenderingCompatListener(
                JankEventReporter(logger),
                Executors.newSingleThreadScheduledExecutor(),
                Handler(Looper.getMainLooper()),
                1500,
            )
        val window = mock<Window>()
        val activity = mock<Activity>()
        whenever(activity.window).thenReturn(window)

        listener.onActivityResumed(activity)
        val frameListener = argumentCaptor<Window.OnFrameMetricsAvailableListener>()
        verify(window).addOnFrameMetricsAvailableListener(frameListener.capture(), any())
        for (ms in listOf(5L, 11L, 101L, 701L, 17L, 17L, 16L, 11L)) {
            val frame = mock<FrameMetrics>()
            whenever(frame.getMetric(FrameMetrics.DRAW_DURATION)).thenReturn(TimeUnit.MILLISECONDS.toNanos(ms))
            frameListener.firstValue.onFrameMetricsAvailable(window, frame, 0)
        }
        listener.onActivityPaused(activity)
        listener.shutdown()

        // 101, 701, 17 and 17 ms frames are slower than 16 ms, and only 701 ms is frozen.
        val events =
            processor.records.map {
                listOf(
                    it.eventName,
                    it.attributes
                        .asMap()
                        .mapKeys { (key, _) -> key.key }
                        .toSortedMap()
                        .toString(),
                )
            }
        assertEquals(
            listOf(
                listOf("app.jank", "{app.jank.frame_count=4, app.jank.period=1.5, app.jank.threshold=0.016}"),
                listOf("app.jank", "{app.jank.frame_count=1, app.jank.period=1.5, app.jank.threshold=0.7}"),
            ),
            events,
        )
    }

    /**
     * End to end: configure the SDK with the real android-agent instrumentations, as an app does,
     * and pause an activity on Android 11. Before the workaround, and when run with
     * -Pandroid11Duration (see DEVELOPING.md), this threw NoSuchMethodError from
     * SlowRenderListener.onActivityPaused.
     */
    @Test
    fun pausingActivityDoesNotCrash() {
        val options =
            HoneycombOptions
                .Builder(HoneycombOptionsMapSource(mapOf("HONEYCOMB_API_KEY" to "key")))
                .build()
        Honeycomb.configure(RuntimeEnvironment.getApplication(), options)

        Robolectric
            .buildActivity(Activity::class.java)
            .create()
            .start()
            .resume()
            .pause()
    }
}
