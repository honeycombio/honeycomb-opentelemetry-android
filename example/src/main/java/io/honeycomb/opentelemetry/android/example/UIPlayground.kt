package io.honeycomb.opentelemetry.android.example

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.honeycomb.opentelemetry.android.example.ui.theme.HoneycombOpenTelemetryAndroidTheme

@Composable
internal fun UIPlayground() {
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        AnimationSpeedTest()
        Spacer(Modifier.height(20.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.startActivity(Intent(context, ClassicActivity::class.java))
            },
        ) {
            Text(text = "Start XML UI")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UIPlaygroundPreview() {
    HoneycombOpenTelemetryAndroidTheme {
        UIPlayground()
    }
}
