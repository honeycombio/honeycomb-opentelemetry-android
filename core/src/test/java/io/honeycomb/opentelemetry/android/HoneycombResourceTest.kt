package io.honeycomb.opentelemetry.android

import org.junit.Assert.*
import org.junit.Test

class HoneycombResourceTest {
    @Test
    fun configure_buildsResourceWithCustomAttributesFromOptions() {
        // Create options with custom resource attributes
        val customAttributes = mapOf("custom.attr" to "custom-value", "bufo" to "is_best")
        val options =
            HoneycombOptions
                .Builder(
                    HoneycombOptionsMapSource(
                        mapOf(
                            "HONEYCOMB_API_KEY" to "test-key",
                            "service.name" to "test-service",
                        ),
                    ),
                ).setResourceAttributes(customAttributes)
                .build()

        assertEquals("unknown_service", options.resourceAttributes["service.name"])
        assertEquals(BuildConfig.HONEYCOMB_DISTRO_VERSION, options.resourceAttributes["honeycomb.distro.version"])
        assertEquals(BuildConfig.HONEYCOMB_DISTRO_VERSION, options.resourceAttributes["telemetry.distro.version"])
        assertEquals("android", options.resourceAttributes["telemetry.sdk.language"])
        assertEquals("io.honeycomb.opentelemetry.android", options.resourceAttributes["telemetry.distro.name"])

        // Verify standard Honeycomb distro attributes are present in options
        assertEquals(BuildConfig.HONEYCOMB_DISTRO_VERSION, options.resourceAttributes["honeycomb.distro.version"])
        assertEquals("android", options.resourceAttributes["telemetry.sdk.language"])
        assertEquals("io.honeycomb.opentelemetry.android", options.resourceAttributes["telemetry.distro.name"])

        // Verify options contain the expected resource attributes
        assertEquals("custom-value", options.resourceAttributes["custom.attr"])
        assertEquals("is_best", options.resourceAttributes["bufo"])
    }
}
