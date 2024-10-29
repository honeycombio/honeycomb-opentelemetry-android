package io.honeycomb.opentelemetry.android.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.honeycomb.opentelemetry.android.example.ui.theme.HoneycombOpenTelemetryAndroidTheme
import io.opentelemetry.android.OpenTelemetryRum
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

val TAG = "MainActivity"

/**
 * An activity with various UI elements that cause telemetry to be emitted.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ExampleApp
        val otelRum = app.otelRum

        enableEdgeToEdge()
        setContent {
            HoneycombOpenTelemetryAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Playground(
                        otelRum,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

private fun onSendSpan(otelRum: OpenTelemetryRum?) {
    val otel = otelRum?.openTelemetry
    val tracer = otel?.getTracer("@honeycombio/smoke-test")
    val span = tracer?.spanBuilder("test-span")?.startSpan()
    Thread.sleep(50)
    span?.end()
}

private fun onSendMetrics(otelRum: OpenTelemetryRum?) {
    val otel = otelRum?.openTelemetry
    val meter = otel?.getMeter("@honeycombio/smoke-test")
    val counter = meter?.counterBuilder("smoke-test.metric.int")?.build()

    counter?.add(1)
}

private fun onSendNetworkRequest(setResponse: (str: String) -> Unit) {
    Log.w(TAG, "making network request")
    setResponse("loading...")

    val client = OkHttpClient.Builder().build()
    val request = Request.Builder()
        .url("http://10.0.2.2:1080/simple-api")
        .headers(Headers.headersOf("content-type", "application/json", "accept", "application/json"))
        .build()
    val callback = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.w(TAG, "OkHttp error response: $e")
            setResponse("error: ${e.message}")
        }
        override fun onResponse(call: Call, response: Response) {
            val body = response.body?.string()
            Log.w(TAG, "OkHttp response: ${response.code}: ${response.message}, $body")
            setResponse("Network Request Succeeded")
            response.close()
        }
    }
    client.newCall(request).enqueue(callback)
}

private fun onANR() {
    // Occupy the main thread long enough for Android to think the app is unresponsive.
    Thread.sleep(10000)
}

private fun onCrash() {
    throw RuntimeException("This crash was intentional.")
}

// This is used below, to make animation take certain lengths of time.
enum class AnimationSpeed(val sleepTime: Long) {
    NORMAL(0),
    SLOW(32),
    FROZEN(1400)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Playground(otel: OpenTelemetryRum?, modifier: Modifier = Modifier) {
    val networkRequestStatus = remember { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize(),
        ) {
        Text(
            text = "The following components demonstrate\n" +
                    "auto-instrumentation features of the\n" +
                    "Honeycomb Android SDK."
        )
        Spacer(modifier = Modifier.height(50.dp))

        // This component is specifically designed to render slowly,
        // to demonstrate slow render detections.
        val angle = rememberInfiniteTransition(
            label = "AngleTransition"
        ).animateFloat(
            initialValue = 0.0f,
            targetValue = 360.0f,
            animationSpec = infiniteRepeatable(
                tween(1000, easing = LinearEasing),
                RepeatMode.Restart),
            label = "AngleAnimation",
        )
        var animationSpeed by remember {
            mutableStateOf(AnimationSpeed.NORMAL)
        }
        Spacer(
            modifier = Modifier
                .height(100.dp)
                .width(100.dp)
                .drawBehind {
                    // This is what makes it slow.
                    if (animationSpeed.sleepTime > 0) {
                        Thread.sleep(animationSpeed.sleepTime)
                    }
                    drawCircle(color = Color.Gray)
                    inset(5.dp.toPx()) {
                        drawCircle(color = Color.Yellow)
                        val top = Offset(center.x, 0.0f)
                        rotate(degrees = angle.value) {
                            drawLine(
                                color = Color.Gray,
                                start = top,
                                end = center,
                                strokeWidth = 5.dp.toPx()
                            )
                        }
                    }
                },
        )
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = animationSpeed == AnimationSpeed.NORMAL,
                onClick = { animationSpeed = AnimationSpeed.NORMAL },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            ) {
                Text(text = "Normal")
            }
            SegmentedButton(
                selected = animationSpeed == AnimationSpeed.SLOW,
                onClick = { animationSpeed = AnimationSpeed.SLOW },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            ) {
                Text(text = "Slow")
            }
            SegmentedButton(
                selected = animationSpeed == AnimationSpeed.FROZEN,
                onClick = { animationSpeed = AnimationSpeed.FROZEN },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            ) {
                Text(text = "Frozen")
            }
        }
        Spacer(modifier = Modifier.height(50.dp))

        Button(onClick = { onSendSpan(otel) }) {
            Text(
                text = "Send Span",
            )
        }
        Button(onClick = { onSendMetrics(otel) }) {
            Text(
                text = "Send Metric",
            )
        }
        Button(onClick = { onANR() }) {
            Text(
                text = "Become Unresponsive (ANR)",
            )
        }
        Button(onClick = { onCrash() }) {
            Text(
                text = "Crash",
            )
        }
        Button(onClick = { onSendNetworkRequest { res -> networkRequestStatus.value = res } }) {
            Text(
                text = "Make a Network Request",
            )
        }
        Text(
            text = networkRequestStatus.value,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlaygroundPreview() {
    HoneycombOpenTelemetryAndroidTheme {
        Playground(null)
    }
}