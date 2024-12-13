package io.honeycomb.opentelemetry.android.example

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.honeycomb.opentelemetry.android.example.ui.theme.HoneycombOpenTelemetryAndroidTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import kotlin.time.TimeSource.Monotonic.markNow

private const val TAG = "ViewInstrumentation"

/**
 * Heavily inspired by https://github.com/theapache64/boil/blob/master/files/LogComposition.kt
 */
@Composable
@Suppress("NOTHING_TO_INLINE")
private inline fun HoneycombInstrumentedComposable(
    name: String,
    composable: @Composable (() -> Unit),
) {
    val tracer = LocalOtelComposition.current!!.openTelemetry.tracerProvider.tracerBuilder("ViewInstrumentationPlayground").build()
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

@Composable
private fun NestedExpensiveView(delayMs: Long) {
    Row {
        HoneycombInstrumentedComposable("nested expensive text") {
            Text(text = timeConsumingCalculation(delayMs))
        }
    }
}

@Composable
private fun DelayedSlider(
    delay: Long,
    onValueChange: (Long) -> Unit,
) {
    val (sliderDelay, setSliderDelay) = remember { mutableFloatStateOf(delay.toFloat()) }
    Slider(
        value = sliderDelay,
        onValueChange = setSliderDelay,
        onValueChangeFinished = { onValueChange(sliderDelay.toLong()) },
        valueRange = 0f..4000f,
        steps = 7,
    )
}

@Composable
private fun ExpensiveView() {
    val (delay, setDelay) = remember { mutableLongStateOf(1000L) }

    HoneycombInstrumentedComposable("main view") {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            DelayedSlider(delay = delay, onValueChange = setDelay)

            HoneycombInstrumentedComposable("expensive text 1") {
                Text(text = timeConsumingCalculation(delay))
            }

            HoneycombInstrumentedComposable("expensive text 2") {
                Text(text = timeConsumingCalculation(delay))
            }

            HoneycombInstrumentedComposable("expensive text 3") {
                Text(text = timeConsumingCalculation(delay))
            }

            HoneycombInstrumentedComposable("nested expensive view") {
                NestedExpensiveView(delayMs = delay)
            }

            HoneycombInstrumentedComposable("expensive text 4") {
                Text(text = timeConsumingCalculation(delay))
            }
        }
    }
}

@Composable
internal fun ViewInstrumentationPlayground() {
    val (enabled, setEnabled) = remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "enable slow render")
            Switch(checked = enabled, onCheckedChange = setEnabled)
        }
        if (enabled) {
            ExpensiveView()
        }
    }
}

private fun timeConsumingCalculation(delayMs: Long): String {
    Log.d(TAG, "starting time consuming calculation")
    Thread.sleep(delayMs)
    return "slow text: ${BigDecimal.valueOf(delayMs / 1000.toDouble()).setScale(2, RoundingMode.HALF_UP)} seconds"
}

@Preview(showBackground = true)
@Composable
fun ViewInstrumentationPlaygroundPreview() {
    HoneycombOpenTelemetryAndroidTheme {
        ViewInstrumentationPlayground()
    }
}
