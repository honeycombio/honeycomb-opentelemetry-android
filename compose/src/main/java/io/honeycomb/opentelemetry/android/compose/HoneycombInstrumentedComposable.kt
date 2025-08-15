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

    val otelRum = LocalOpenTelemetryRum.current!!.openTelemetry
    val tracer = otelRum.tracerProvider.tracerBuilder("io.honeycomb.view").build()
    val span =
        tracer
            .spanBuilder("View Render")
            .setAttribute("view.name", name)
            .startSpan()

    span.makeCurrent().use {
        val bodySpan =
            tracer
                .spanBuilder("View Body")
                .setAttribute("view.name", name)
                .startSpan()

        bodySpan.makeCurrent().use {
            val start = markNow()
            composable()
            val endTime = System.currentTimeMillis()

            val bodyDuration = start.elapsedNow()
            // bodyDuration is in seconds
            // calling duration.inWholeSeconds would lose precision
            span.setAttribute("view.renderDuration", bodyDuration.toDouble(DurationUnit.SECONDS))

            SideEffect {
                bodySpan.end(endTime, TimeUnit.MILLISECONDS)
                val renderDuration = start.elapsedNow()
                span.setAttribute("view.totalDuration", renderDuration.toDouble(DurationUnit.SECONDS))
                span.end()
            }
        }
    }
}

val LocalOpenTelemetryRum = compositionLocalOf<OpenTelemetryRum?> { null }
