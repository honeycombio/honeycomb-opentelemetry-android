package io.honeycomb.opentelemetry.android.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import kotlin.time.measureTime

@Composable
private fun HoneycombInstrumentedComposable(
    name: String,
    composable: @Composable (() -> Unit),
) {
    val tracer = LocalOtelComposition.current!!.openTelemetry.tracerProvider.tracerBuilder("ViewInstrumentationPlayground").build()
    val span = tracer.spanBuilder("Render").setAttribute("view.name", name).startSpan()
    span.makeCurrent()

    val duration =
        measureTime {
            composable()
        }

    // renderDuration is in seconds
    // calling duration.inWholeSeconds would lose precision
    span.setAttribute("view.renderDuration", duration.inWholeMicroseconds / 1_000_000.toDouble())

    span.end()
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
        HoneycombInstrumentedComposable("nested expensive composable") {
            NestedExpensiveView(delayMs = delay)
        }
        HoneycombInstrumentedComposable("expensive text 4") {
            Text(text = timeConsumingCalculation(delay))
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
    println("starting time consuming calculation")
    Thread.sleep(delayMs)
    return "slow text: ${BigDecimal.valueOf(delayMs / 1000).setScale(2, RoundingMode.HALF_UP)} seconds"
}

@Preview(showBackground = true)
@Composable
fun ViewInstrumentationPlaygroundPreview() {
    HoneycombOpenTelemetryAndroidTheme {
        ViewInstrumentationPlayground()
    }
}
