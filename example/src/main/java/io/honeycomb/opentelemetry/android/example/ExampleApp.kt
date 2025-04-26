package io.honeycomb.opentelemetry.android.example

import android.app.Application
import io.honeycomb.opentelemetry.android.Honeycomb
import io.honeycomb.opentelemetry.android.HoneycombOptions
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.sdk.OpenTelemetrySdk
import java.lang.Thread.UncaughtExceptionHandler

// An exception that won't crash the app if it isn't caught.
// This is used for testing crash instrumentation.
class SmokeTestException(
    message: String?,
) : Exception(message)

/*
 * In order to test the uncaught exception handler, we need to be able to throw some exceptions and
 * handle them in between when the OpenTelemetry UncaughtExceptionHandler runs and when they would
 * actually crash the apps.
 */
class SmokeTestExceptionHandler : UncaughtExceptionHandler {
    var existingHandler: Thread.UncaughtExceptionHandler? = null

    override fun uncaughtException(
        t: Thread,
        e: Throwable,
    ) {
        if (e !is SmokeTestException) {
            existingHandler?.uncaughtException(t, e)
        }
    }

    fun install() {
        existingHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
}

class ExampleApp : Application() {
    var otelRum: OpenTelemetryRum? = null

    override fun onCreate() {
        super.onCreate()

        SmokeTestExceptionHandler().install()

        // To use this sample app with Honeycomb, change the api key to your own key, and remove
        // the call to setApiEndpoint.
        val options =
            HoneycombOptions
                .builder(this)
                .setApiKey("test-key")
                .setApiEndpoint("http://10.0.2.2:4318")
                .setServiceName("android-test")
                .setServiceVersion("0.0.1")
                .setMetricsDataset("android-test-metrics")
                .setSpanProcessor(SimpleSpanProcessor())
                .setDebug(true)
                .build()

        otelRum = Honeycomb.configure(this, options)
    }

    fun flush() {
        val otel = otelRum?.openTelemetry as OpenTelemetrySdk
        otel.sdkTracerProvider.forceFlush()
        otel.sdkLoggerProvider.forceFlush()
        otel.shutdown()
        this.otelRum = null
        // Theoretically, flushing and shutting down should be sufficient.
        // But empirically, if we don't sleep, sometimes the traces don't show up.
        Thread.sleep(500)
    }
}
