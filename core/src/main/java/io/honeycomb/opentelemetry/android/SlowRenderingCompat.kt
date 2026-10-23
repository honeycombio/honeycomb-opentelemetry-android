package io.honeycomb.opentelemetry.android

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import androidx.annotation.RequiresApi
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/*
 * Temporary workaround for a crash in the upstream slow rendering instrumentation.
 *
 * OpenTelemetry Android's slow rendering instrumentation (1.0.0 - 1.9.0 at least) calls
 * java.time.Duration.toSeconds(), which only exists on Android 12 (API 31) and above. On Android
 * 7 - 11 it throws NoSuchMethodError from Activity.onPause() and crashes the host app.
 *
 * On those versions, Honeycomb.configure() suppresses the upstream "slowrendering" instrumentation
 * and installs this copy of it instead. The only functional change is how the polling interval is
 * converted to seconds. Delete this file, and its use in Honeycomb.kt, once an upstream release
 * with the fix is the minimum version.
 *
 * Each class below is condensed from the upstream file linked just above it, at the commit tagged
 * v1.0.1 (the version this SDK depends on).
 *
 * The upstream code is Apache-2.0 licensed, Copyright The OpenTelemetry Authors, like this repo.
 */

internal const val UPSTREAM_SLOW_RENDERING_NAME = "slowrendering"
private const val UPSTREAM_SLOW_RENDERING_CLASS =
    "io.opentelemetry.android.instrumentation.slowrendering.SlowRenderingInstrumentation"
private const val SLOW_THRESHOLD_MS = 16
private const val FROZEN_THRESHOLD_MS = 700
private const val POLL_INTERVAL_MS = 1000L

private val FRAME_COUNT: AttributeKey<Long> = AttributeKey.longKey("app.jank.frame_count")
private val PERIOD: AttributeKey<Double> = AttributeKey.doubleKey("app.jank.period")
private val THRESHOLD: AttributeKey<Double> = AttributeKey.doubleKey("app.jank.threshold")

private val NANOS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1).toInt()
private val NANOS_ROUNDING_VALUE = NANOS_PER_MS / 2

/**
 * Whether the upstream slow rendering instrumentation would be installed on a device where it
 * crashes, so that [SlowRenderingCompatInstrumentation] has to take its place.
 */
internal fun shouldReplaceUpstreamSlowRendering(
    sdkInt: Int,
    disabledInstrumentation: List<String>,
): Boolean {
    if (sdkInt < Build.VERSION_CODES.N || sdkInt >= Build.VERSION_CODES.S) {
        return false
    }
    if (UPSTREAM_SLOW_RENDERING_NAME in disabledInstrumentation) {
        return false
    }
    return try {
        Class.forName(UPSTREAM_SLOW_RENDERING_CLASS)
        true
    } catch (_: ClassNotFoundException) {
        false
    }
}

/**
 * Copied from https://github.com/open-telemetry/opentelemetry-android/blob/07ffe08f9e3fd02a776853bd0edea014805f2c9f/instrumentation/slowrendering/src/main/java/io/opentelemetry/android/instrumentation/slowrendering/SlowRenderingInstrumentation.kt
 *
 * A copy of the upstream slow rendering instrumentation that is safe on Android 7 - 11.
 */
@RequiresApi(Build.VERSION_CODES.N)
internal class SlowRenderingCompatInstrumentation : AndroidInstrumentation {
    @Volatile
    private var listener: SlowRenderingCompatListener? = null

    override val name: String = "honeycomb.slowrendering"

    override fun install(ctx: InstallationContext) {
        if (listener != null) {
            return
        }
        val logger = ctx.openTelemetry.logsBridge.get("app.jank")
        val reporter = JankEventReporter(logger)
        val newListener =
            SlowRenderingCompatListener(
                reporter,
                Executors.newScheduledThreadPool(1),
                Handler(frameMetricsLooper()),
                POLL_INTERVAL_MS,
            )
        listener = newListener
        ctx.application?.registerActivityLifecycleCallbacks(newListener)
        newListener.start()
    }

    override fun uninstall(ctx: InstallationContext) {
        val current = listener ?: return
        ctx.application?.unregisterActivityLifecycleCallbacks(current)
        current.shutdown()
        listener = null
    }

    private companion object {
        val frameMetricsThread = HandlerThread("HoneycombFrameMetricsCollector")

        fun frameMetricsLooper(): Looper {
            if (!frameMetricsThread.isAlive) {
                frameMetricsThread.start()
            }
            return frameMetricsThread.looper
        }
    }
}

/**
 * Copied from https://github.com/open-telemetry/opentelemetry-android/blob/07ffe08f9e3fd02a776853bd0edea014805f2c9f/instrumentation/slowrendering/src/main/java/io/opentelemetry/android/instrumentation/slowrendering/EventJankReporter.kt
 *
 * Emits one "app.jank" event per threshold (slow and frozen) with the number of frames that
 * took longer than the threshold.
 */
internal class JankEventReporter(
    private val logger: Logger,
) {
    fun reportSlow(
        durationToCountHistogram: Map<Int, Int>,
        periodSeconds: Double,
    ) {
        for (thresholdMs in listOf(SLOW_THRESHOLD_MS, FROZEN_THRESHOLD_MS)) {
            val threshold = thresholdMs / 1000.0
            val frameCount =
                durationToCountHistogram
                    .filterKeys { it / 1000.0 > threshold }
                    .values
                    .sumOf { it.toLong() }
            if (frameCount > 0) {
                logger
                    .logRecordBuilder()
                    .setEventName("app.jank")
                    .setAllAttributes(
                        Attributes
                            .builder()
                            .put(FRAME_COUNT, frameCount)
                            .put(PERIOD, periodSeconds)
                            .put(THRESHOLD, threshold)
                            .build(),
                    ).emit()
            }
        }
    }
}

/**
 * Copied from https://github.com/open-telemetry/opentelemetry-android/blob/07ffe08f9e3fd02a776853bd0edea014805f2c9f/instrumentation/slowrendering/src/main/java/io/opentelemetry/android/instrumentation/slowrendering/SlowRenderListener.kt
 */
@RequiresApi(Build.VERSION_CODES.N)
internal class SlowRenderingCompatListener(
    private val reporter: JankEventReporter,
    private val executor: ScheduledExecutorService,
    private val frameMetricsHandler: Handler,
    private val pollIntervalMs: Long,
) : Application.ActivityLifecycleCallbacks {
    private val activities = ConcurrentHashMap<Activity, FrameDurations>()

    // Not Duration.toSeconds(): that is the call that does not exist before Android 12.
    private val pollIntervalSeconds = pollIntervalMs / 1000.0

    fun start() {
        executor.scheduleWithFixedDelay(
            { reportSlowRenders() },
            pollIntervalMs,
            pollIntervalMs,
            TimeUnit.MILLISECONDS,
        )
    }

    fun shutdown() {
        executor.shutdownNow()
        for ((activity, frameDurations) in activities) {
            activity.window.removeOnFrameMetricsAvailableListener(frameDurations)
        }
        activities.clear()
    }

    override fun onActivityResumed(activity: Activity) {
        if (executor.isShutdown) {
            return
        }
        val frameDurations = FrameDurations()
        if (activities.putIfAbsent(activity, frameDurations) == null) {
            activity.window.addOnFrameMetricsAvailableListener(frameDurations, frameMetricsHandler)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (executor.isShutdown) {
            return
        }
        val frameDurations = activities.remove(activity) ?: return
        activity.window.removeOnFrameMetricsAvailableListener(frameDurations)
        reporter.reportSlow(frameDurations.reset(), pollIntervalSeconds)
    }

    private fun reportSlowRenders() {
        try {
            for (frameDurations in activities.values) {
                reporter.reportSlow(frameDurations.reset(), pollIntervalSeconds)
            }
        } catch (e: Exception) {
            android.util.Log.w("OpenTelemetryRum", "Exception while processing frame metrics", e)
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {}

    override fun onActivityDestroyed(activity: Activity) {}
}

/**
 * Copied from https://github.com/open-telemetry/opentelemetry-android/blob/07ffe08f9e3fd02a776853bd0edea014805f2c9f/instrumentation/slowrendering/src/main/java/io/opentelemetry/android/instrumentation/slowrendering/PerActivityListener.kt
 *
 * Collects a histogram of draw durations (in ms) for one activity.
 */
@RequiresApi(Build.VERSION_CODES.N)
internal class FrameDurations : Window.OnFrameMetricsAvailableListener {
    private val lock = Any()
    private var histogram = HashMap<Int, Int>()

    override fun onFrameMetricsAvailable(
        window: Window?,
        frameMetrics: FrameMetrics,
        dropCountSinceLastInvocation: Int,
    ) {
        if (frameMetrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L) {
            return
        }
        val drawDurationNs = frameMetrics.getMetric(FrameMetrics.DRAW_DURATION)
        if (drawDurationNs >= 0) {
            val durationMs = ((drawDurationNs + NANOS_ROUNDING_VALUE) / NANOS_PER_MS).toInt()
            synchronized(lock) {
                histogram[durationMs] = (histogram[durationMs] ?: 0) + 1
            }
        }
    }

    fun reset(): Map<Int, Int> =
        synchronized(lock) {
            val old = histogram
            histogram = HashMap()
            old
        }
}
