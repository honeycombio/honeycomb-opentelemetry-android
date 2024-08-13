package io.honeycomb.opentelemetry.android

import android.app.Application
import android.content.res.Resources
import io.opentelemetry.api.GlobalOpenTelemetry
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class HoneycombSmokeTest {
    @Test
    fun collector_works() {
        val mockResources = mock<Resources> {
            on { getFloat(anyInt()) } doReturn 0.0f
            on { getInteger(anyInt()) } doReturn 0
            on { getString(anyInt()) } doReturn ""
        }
        val mockApp = mock<Application> {
            on { packageName } doReturn "test-package"
            on { resources } doReturn mockResources
        }

        val options = HoneycombOptions.builder(mockApp)
            .setApiKey("test-key")
            .setApiEndpoint("http://localhost:4318")
            .build()

        Honeycomb.configure(mockApp, options)

        val tracer = GlobalOpenTelemetry.getTracer("smoke-test")
        tracer.spanBuilder("test-span").startSpan().end()
    }
}
