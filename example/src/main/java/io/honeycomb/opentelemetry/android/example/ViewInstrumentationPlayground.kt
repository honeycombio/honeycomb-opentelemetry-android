package io.honeycomb.opentelemetry.android.example

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.honeycomb.opentelemetry.android.example.ui.theme.HoneycombOpenTelemetryAndroidTheme
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
private fun NestedExpensiveView(delayMs: Long) {
    Row {
        Text(text = timeConsumingCalculation(delayMs))
    }
}

@Composable
private fun DelayedSlider(delay: Long, onValueChange: (Long) -> Unit) {
    val (sliderDelay, setSliderDelay) = remember { mutableFloatStateOf(delay.toFloat()) }
    Slider(
        value = sliderDelay,
        onValueChange = setSliderDelay,
        onValueChangeFinished = { onValueChange(sliderDelay.toLong()) },
        valueRange = 0f..4000f,
        steps = 7
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
        Text(text = timeConsumingCalculation(delay))
        Text(text = timeConsumingCalculation(delay))
        Text(text = timeConsumingCalculation(delay))
        NestedExpensiveView(delayMs = delay)
        Text(text = timeConsumingCalculation(delay))
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
            modifier = Modifier.fillMaxWidth()
        ){
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
