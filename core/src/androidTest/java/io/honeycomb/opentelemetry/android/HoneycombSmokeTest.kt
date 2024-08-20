package io.honeycomb.opentelemetry.android

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.junit.AfterClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HoneycombSmokeTest {
    companion object {
        val openTelemetryRum: OpenTelemetryRum by lazy {
            val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application

            val options = HoneycombOptions.builder(app)
                .setApiKey("test-key")
                .setApiEndpoint("http://10.0.2.2:4318")
                .setServiceName("android-test")
                .build()

            Honeycomb.configure(app, options)
        }

        @AfterClass @JvmStatic fun tearDown() {
            val otel = openTelemetryRum.openTelemetry as OpenTelemetrySdk
            val tracerProvider = otel.sdkTracerProvider
            tracerProvider.forceFlush()
            otel.shutdown()
            // Theoretically, flushing and shutting down should be sufficient.
            // But empirically, if we don't sleep, sometimes the traces don't show up.
            Thread.sleep(500)
        }
    }

    @Test
    fun span_works() {
        val otel = openTelemetryRum.openTelemetry
        val tracer = otel.getTracer("@honeycombio/smoke-test")
        val span = tracer.spanBuilder("test-span").startSpan()
        Thread.sleep(50)
        span.end()
   }
}
