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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import io.honeycomb.opentelemetry.android.compose.HoneycombInstrumentedComposable
import io.honeycomb.opentelemetry.android.example.ui.theme.HoneycombOpenTelemetryAndroidTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

private const val TAG = "ViewInstrumentation"

@Composable
private fun NestedExpensiveView(delay: Duration) {
    Row {
        HoneycombInstrumentedComposable("nested expensive text") {
            Text(text = timeConsumingCalculation(delay))
        }
    }
}

@Composable
private fun DelayedSlider(
    delay: Long,
    onValueChange: (Duration) -> Unit,
) {
    val (sliderDelay, setSliderDelay) = remember { mutableFloatStateOf(delay.toFloat()) }
    Slider(
        value = sliderDelay,
        onValueChange = setSliderDelay,
        onValueChangeFinished = { onValueChange(sliderDelay.toLong().milliseconds) },
        valueRange = 0f..4000f,
        steps = 7,
    )
}

@Composable
private fun ExpensiveView() {
    val (delay, setDelay) = remember { mutableStateOf(1000L.milliseconds) }

    HoneycombInstrumentedComposable("main view") {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            DelayedSlider(delay = delay.toLong(DurationUnit.MILLISECONDS), onValueChange = setDelay)

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
                NestedExpensiveView(delay = delay)
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
            Switch(
                checked = enabled,
                onCheckedChange = setEnabled,
                modifier = Modifier.testTag("slow_render_switch"),
            )
        }
        if (enabled) {
            ExpensiveView()
        }
    }
}

private fun timeConsumingCalculation(delay: Duration): String {
    Log.d(TAG, "starting time consuming calculation")
    Thread.sleep(delay.toLong(DurationUnit.MILLISECONDS))
    return "slow text: ${delay.toDouble(DurationUnit.SECONDS)} seconds"
}

@Preview(showBackground = true)
@Composable
fun ViewInstrumentationPlaygroundPreview() {
    HoneycombOpenTelemetryAndroidTheme {
        ViewInstrumentationPlayground()
    }
}
