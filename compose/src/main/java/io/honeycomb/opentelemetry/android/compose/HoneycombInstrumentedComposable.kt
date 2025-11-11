package io.honeycomb.opentelemetry.android.compose

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import io.opentelemetry.android.OpenTelemetryRum
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit
import kotlin.time.TimeSource.Monotonic.markNow

private const val TAG = "HoneycombInstrumentedCo" // max 23 characters :sob:

/**
 * Heavily inspired by https://github.com/theapache64/boil/blob/master/files/LogComposition.kt
 */
@Composable
fun HoneycombInstrumentedComposable(
    name: String,
    composable: @Composable (() -> Unit),
) {
    if (LocalOpenTelemetryRum.current == null) {
        Log.w(TAG, "No LocalOpenTelemetryRum provided!")

        composable()
        return
    }

    val otelRum = LocalOpenTelemetryRum.current!!.getOpenTelemetry()
    val tracer = otelRum.tracerProvider.tracerBuilder("io.honeycomb.view").build()
    val renderSpan =
        tracer
            .spanBuilder("View Render")
            .setAttribute("view.name", name)
            .startSpan()

    renderSpan.makeCurrent().use {
        val bodySpan =
            tracer
                .spanBuilder("View Body")
                .setAttribute("view.name", name)
                .startSpan()

        val start = markNow()
        bodySpan.makeCurrent().use {
            composable()
        }
        bodySpan.end()

        val bodyDuration = start.elapsedNow()
        // bodyDuration is in seconds
        // calling duration.inWholeSeconds would lose precision
        renderSpan.setAttribute("view.renderDuration", bodyDuration.toDouble(DurationUnit.SECONDS))

        SideEffect {
            val renderDuration = start.elapsedNow()
            renderSpan.setAttribute("view.totalDuration", renderDuration.toDouble(DurationUnit.SECONDS))
            renderSpan.end()
        }
    }
}

val LocalOpenTelemetryRum = compositionLocalOf<OpenTelemetryRum?> { null }
