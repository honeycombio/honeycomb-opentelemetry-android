package io.honeycomb.opentelemetry.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import io.opentelemetry.api.OpenTelemetry
import java.time.Instant
import kotlin.time.TimeSource.Monotonic.markNow

/**
 * Heavily inspired by https://github.com/theapache64/boil/blob/master/files/LogComposition.kt
 */
@Composable
fun HoneycombInstrumentedComposable(
    name: String,
    otelRum: OpenTelemetry,
    composable: @Composable (() -> Unit),
) {
    val tracer = otelRum.tracerProvider.tracerBuilder("io.honeycomb.render-instrumentation").build()
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
            val endTime = Instant.now()

            val bodyDuration = start.elapsedNow()
            // bodyDuration is in seconds
            // calling duration.inWholeSeconds would lose precision
            span.setAttribute("view.renderDuration", bodyDuration.inWholeMicroseconds / 1_000_000.toDouble())

            SideEffect {
                bodySpan.end(endTime)
                val renderDuration = start.elapsedNow()
                span.setAttribute("view.totalDuration", renderDuration.inWholeMicroseconds / 1_000_000.toDouble())
                span.end()
            }
        }
    }
}
