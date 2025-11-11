package io.honeycomb.opentelemetry.android.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.honeycomb.opentelemetry.android.Honeycomb
import io.honeycomb.opentelemetry.android.example.ui.theme.HoneycombOpenTelemetryAndroidTheme
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.sdk.OpenTelemetrySdk

private fun onSendSpan(otelRum: OpenTelemetryRum?) {
    val otel = otelRum?.getOpenTelemetry()
    val tracer = otel?.getTracer("io.honeycomb.smoke-test")
    val baggage =
        Baggage
            .builder()
            .put("baggage-key", "baggage-value")
            .build()
    baggage.makeCurrent().use {
        val span = tracer?.spanBuilder("test-span")?.startSpan()
        Thread.sleep(50)
        span?.end()
    }
}

private fun onSendLog(otelRum: OpenTelemetryRum?) {
    val sdk = otelRum?.getOpenTelemetry() as OpenTelemetrySdk
    val loggerProvider = sdk.sdkLoggerProvider
    val logger: Logger = loggerProvider.loggerBuilder("io.honeycomb.smoke-test").build()
    val baggage =
        Baggage
            .builder()
            .put("baggage-key", "baggage-value")
            .build()
    baggage.makeCurrent().use {
        logger.logRecordBuilder().emit()
    }
}

private fun onSendMetrics(otelRum: OpenTelemetryRum?) {
    val otel = otelRum?.getOpenTelemetry()
    val meter = otel?.getMeter("io.honeycomb.smoke-test")
    val counter = meter?.counterBuilder("smoke-test.metric.int")?.build()

    counter?.add(1)
}

private fun onANR() {
    // Occupy the main thread long enough for Android to think the app is unresponsive.
    Thread.sleep(10000)
}

private fun onLogException(otelRum: OpenTelemetryRum?) {
    try {
        throw RuntimeException("This exception was intentional.")
    } catch (e: Exception) {
        if (otelRum != null) {
            Honeycomb.logException(
                otelRum,
                e,
                Attributes.of(
                    AttributeKey.stringKey("user.name"),
                    "bufo",
                    AttributeKey.longKey("user.id"),
                    1,
                ),
                Thread.currentThread(),
            )
        }
    }
}

private fun onCrash() {
    throw RuntimeException("This crash was intentional.")
}

private fun onSimulateCrash() {
    // If we throw the exception, there is no way to avoid a crash.
    // But if we call the uncaught exception handler directly, we can intercept the exception
    // before the app is killed.
    val handler = Thread.getDefaultUncaughtExceptionHandler()
    val thread = Thread.currentThread()
    val exception = SmokeTestException("This crash was intentional.")
    handler?.uncaughtException(thread, exception)
}

@Composable
internal fun CorePlayground(otel: OpenTelemetryRum? = null) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onSendSpan(otel) }) {
            Text(
                text = "Send Span",
            )
        }
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onSendLog(otel) }) {
            Text(
                text = "Send Log",
            )
        }
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onSendMetrics(otel) }) {
            Text(
                text = "Send Metric",
            )
        }
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onANR() }) {
            Text(
                text = "Become Unresponsive (ANR)",
            )
        }
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onLogException(otel) }) {
            Text(
                text = "Log Exception",
            )
        }
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onCrash() }) {
            Text(
                text = "Crash",
            )
        }
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onSimulateCrash() }) {
            Text(
                text = "Simulate Crash",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CorePlaygroundPreview() {
    HoneycombOpenTelemetryAndroidTheme {
        CorePlayground()
    }
}
