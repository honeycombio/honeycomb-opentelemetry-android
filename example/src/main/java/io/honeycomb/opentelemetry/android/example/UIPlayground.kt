package io.honeycomb.opentelemetry.android.example

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.honeycomb.opentelemetry.android.example.ui.theme.HoneycombOpenTelemetryAndroidTheme

@Composable
internal fun UIPlayground() {
    AnimationSpeedTest()
}

@Preview(showBackground = true)
@Composable
fun UIPlaygroundPreview() {
    HoneycombOpenTelemetryAndroidTheme {
        UIPlayground()
    }
}
