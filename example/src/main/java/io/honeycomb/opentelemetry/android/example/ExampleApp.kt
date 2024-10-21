package io.honeycomb.opentelemetry.android.example

import android.app.Application
import io.honeycomb.opentelemetry.android.Honeycomb
import io.honeycomb.opentelemetry.android.HoneycombOptions
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.sdk.OpenTelemetrySdk

class ExampleApp: Application() {
    var otelRum: OpenTelemetryRum? = null

    override fun onCreate() {
        super.onCreate()

        // To use this sample app with Honeycomb, change the api key to your own key, and remove
        // the call to setApiEndpoint.
        val options = HoneycombOptions.builder(this)
            .setApiKey("test-key")
            .setApiEndpoint("http://10.0.2.2:4318")
            .setServiceName("android-test")
            .setMetricsDataset("android-test-metrics")
            .setDebug(true)
            .build()

        otelRum = Honeycomb.configure(this, options)
    }

    fun flush() {
        val otel = otelRum?.openTelemetry as OpenTelemetrySdk
        val tracerProvider = otel.sdkTracerProvider
        tracerProvider.forceFlush()
        otel.shutdown()
        this.otelRum = null
        // Theoretically, flushing and shutting down should be sufficient.
        // But empirically, if we don't sleep, sometimes the traces don't show up.
        Thread.sleep(500)
    }
}
