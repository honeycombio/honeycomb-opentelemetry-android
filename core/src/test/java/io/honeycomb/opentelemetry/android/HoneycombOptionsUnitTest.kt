package io.honeycomb.opentelemetry.android

import org.junit.Assert.*
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * An implementation of OpenTelemetryConfigSource that reads from a HashMap, so that unit tests can
 * run in the JVM, rather than requiring Android-specific classes.
 */
class HoneycombOptionsMapSource(
    private val data: Map<String, Any?>,
) : HoneycombOptionsSource {
    override fun getString(key: String): String? {
        val value = data[key] ?: return null
        if (value !is String) {
            throw IllegalArgumentException("$value is not a String")
        }
        if (value.isBlank()) {
            return null
        }
        return value.trim()
    }

    override fun getInt(key: String): Int? {
        val value = data[key] ?: return null
        if (value !is Int) {
            throw IllegalArgumentException("$value is not an Int")
        }
        return value
    }

    override fun getBoolean(key: String): Boolean? {
        val value = data[key] ?: return null
        if (value !is Boolean) {
            throw IllegalArgumentException("$value is not a Boolean")
        }
        return value
    }
}

/**
 * Tests for loading a config from a source of String -> Any?.
 */
class HoneycombOptionsUnitTest {
    @Test
    fun options_requireHoneycombKey() {
        val data = hashMapOf<String, Any?>()
        assertThrows(HoneycombException::class.java) {
            HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()
        }
    }

    @Test
    fun options_hasCorrectDefaults() {
        val data = hashMapOf<String, Any?>("HONEYCOMB_API_KEY" to "key")
        val options = HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()

        assertEquals("unknown_service", options.serviceName)
        assertEquals(
            mapOf(
                "service.name" to "unknown_service",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )

        assertEquals("https://api.honeycomb.io:443/v1/traces", options.tracesEndpoint)
        assertEquals("https://api.honeycomb.io:443/v1/metrics", options.metricsEndpoint)
        assertEquals("https://api.honeycomb.io:443/v1/logs", options.logsEndpoint)

        val expectedHeaders =
            mapOf(
                "x-otlp-version" to io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION,
                "x-honeycomb-team" to "key",
            )
        assertEquals(expectedHeaders, options.tracesHeaders)
        assertEquals(expectedHeaders, options.metricsHeaders)
        assertEquals(expectedHeaders, options.logsHeaders)

        assertEquals(10.seconds, options.tracesTimeout)
        assertEquals(10.seconds, options.metricsTimeout)
        assertEquals(10.seconds, options.logsTimeout)

        assertEquals(OtlpProtocol.HTTP_PROTOBUF, options.tracesProtocol)
        assertEquals(OtlpProtocol.HTTP_PROTOBUF, options.metricsProtocol)
        assertEquals(OtlpProtocol.HTTP_PROTOBUF, options.logsProtocol)

        assertEquals(1, options.sampleRate)
        assertFalse(options.debug)

        assertFalse(options.offlineCachingEnabled)
    }

    @Test
    fun options_handlesEmptyStrings() {
        val data =
            hashMapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
                "OTEL_SERVICE_NAME" to "",
                "OTEL_SERVICE_VERSION" to "1",
                "OTEL_RESOURCE_ATTRIBUTES" to "",
                "OTEL_EXPORTER_OTLP_HEADERS" to "",
                "OTEL_EXPORTER_OTLP_TIMEOUT" to null,
                "OTEL_EXPORTER_OTLP_PROTOCOL" to "",
            )
        val options = HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()

        assertEquals("unknown_service", options.serviceName)
        assertEquals("1", options.serviceVersion)
        assertEquals(
            mapOf(
                "service.name" to "unknown_service",
                "service.version" to "1",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )

        assertEquals("https://api.honeycomb.io:443/v1/traces", options.tracesEndpoint)
        assertEquals("https://api.honeycomb.io:443/v1/metrics", options.metricsEndpoint)
        assertEquals("https://api.honeycomb.io:443/v1/logs", options.logsEndpoint)

        val expectedHeaders =
            mapOf<String, String>(
                "x-otlp-version" to io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION,
                "x-honeycomb-team" to "key",
            )
        assertEquals(expectedHeaders, options.tracesHeaders)
        assertEquals(expectedHeaders, options.metricsHeaders)
        assertEquals(expectedHeaders, options.logsHeaders)

        assertEquals(10.seconds, options.tracesTimeout)
        assertEquals(10.seconds, options.metricsTimeout)
        assertEquals(10.seconds, options.logsTimeout)

        assertEquals(OtlpProtocol.HTTP_PROTOBUF, options.tracesProtocol)
        assertEquals(OtlpProtocol.HTTP_PROTOBUF, options.metricsProtocol)
        assertEquals(OtlpProtocol.HTTP_PROTOBUF, options.logsProtocol)
    }

    @Test
    fun options_usesFallbacks() {
        val data =
            hashMapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
                "HONEYCOMB_API_ENDPOINT" to "http://example.com:1234",
                "OTEL_SERVICE_NAME" to "service",
                "OTEL_SERVICE_VERSION" to "1",
                "OTEL_RESOURCE_ATTRIBUTES" to "resource=aaa",
                "OTEL_EXPORTER_OTLP_HEADERS" to "header=bbb",
                "OTEL_EXPORTER_OTLP_TIMEOUT" to 30000,
                "OTEL_EXPORTER_OTLP_PROTOCOL" to "http/json",
            )
        val options = HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()
        assertEquals("service", options.serviceName)
        assertEquals("1", options.serviceVersion)
        assertEquals(
            mapOf(
                "resource" to "aaa",
                "service.name" to "service",
                "service.version" to "1",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )

        assertEquals("http://example.com:1234/v1/traces", options.tracesEndpoint)
        assertEquals("http://example.com:1234/v1/metrics", options.metricsEndpoint)
        assertEquals("http://example.com:1234/v1/logs", options.logsEndpoint)

        val expectedHeaders =
            mapOf(
                "header" to "bbb",
                "x-honeycomb-team" to "key",
                "x-otlp-version" to io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION,
            )
        assertEquals(expectedHeaders, options.tracesHeaders)
        assertEquals(expectedHeaders, options.metricsHeaders)
        assertEquals(expectedHeaders, options.logsHeaders)

        assertEquals(30.seconds, options.tracesTimeout)
        assertEquals(30.seconds, options.metricsTimeout)
        assertEquals(30.seconds, options.logsTimeout)

        assertEquals(OtlpProtocol.HTTP_JSON, options.tracesProtocol)
        assertEquals(OtlpProtocol.HTTP_JSON, options.metricsProtocol)
        assertEquals(OtlpProtocol.HTTP_JSON, options.logsProtocol)
    }

    @Test
    fun options_usesAllValues() {
        val data =
            hashMapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
                "HONEYCOMB_DATASET" to "dataset",
                "HONEYCOMB_METRICS_DATASET" to "metrics",
                "HONEYCOMB_TRACES_APIKEY" to "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "HONEYCOMB_METRICS_APIKEY" to "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                "HONEYCOMB_LOGS_APIKEY" to "cccccccccccccccccccccccccccccccc",
                "HONEYCOMB_TRACES_ENDPOINT" to "http://traces.example.com:1234",
                "HONEYCOMB_METRICS_ENDPOINT" to "http://metrics.example.com:1234",
                "HONEYCOMB_LOGS_ENDPOINT" to "http://logs.example.com:1234",
                "SAMPLE_RATE" to 42,
                "DEBUG" to true,
                "OTEL_SERVICE_NAME" to "service",
                "OTEL_SERVICE_VERSION" to "1",
                "OTEL_RESOURCE_ATTRIBUTES" to "resource=aaa",
                "OTEL_EXPORTER_OTLP_TIMEOUT" to 30000,
                "OTEL_EXPORTER_OTLP_PROTOCOL" to "http/json",
                "OTEL_EXPORTER_OTLP_TRACES_HEADERS" to "header=ttt",
                "OTEL_EXPORTER_OTLP_TRACES_TIMEOUT" to 40000,
                "OTEL_EXPORTER_OTLP_TRACES_PROTOCOL" to "http/json",
                "OTEL_EXPORTER_OTLP_METRICS_HEADERS" to "header=mmm",
                "OTEL_EXPORTER_OTLP_METRICS_TIMEOUT" to 50000,
                "OTEL_EXPORTER_OTLP_METRICS_PROTOCOL" to "http/json",
                "OTEL_EXPORTER_OTLP_LOGS_HEADERS" to "header=lll",
                "OTEL_EXPORTER_OTLP_LOGS_TIMEOUT" to 60000,
                "OTEL_EXPORTER_OTLP_LOGS_PROTOCOL" to "http/json",
                "OFFLINE_CACHING_ENABLED" to true,
            )
        val options = HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()

        assertEquals("service", options.serviceName)
        assertEquals("1", options.serviceVersion)
        assertEquals(
            mapOf(
                "service.name" to "service",
                "service.version" to "1",
                "resource" to "aaa",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )

        assertEquals("http://traces.example.com:1234", options.tracesEndpoint)
        assertEquals("http://metrics.example.com:1234", options.metricsEndpoint)
        assertEquals("http://logs.example.com:1234", options.logsEndpoint)

        assertEquals(
            mapOf(
                "header" to "ttt",
                "x-honeycomb-dataset" to "dataset",
                "x-honeycomb-team" to "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "x-otlp-version" to io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION,
            ),
            options.tracesHeaders,
        )
        assertEquals(
            mapOf(
                "header" to "mmm",
                "x-honeycomb-dataset" to "metrics",
                "x-honeycomb-team" to "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                "x-otlp-version" to io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION,
            ),
            options.metricsHeaders,
        )
        assertEquals(
            mapOf(
                "header" to "lll",
                "x-honeycomb-dataset" to "dataset",
                "x-honeycomb-team" to "cccccccccccccccccccccccccccccccc",
                "x-otlp-version" to io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION,
            ),
            options.logsHeaders,
        )

        assertEquals(40.seconds, options.tracesTimeout)
        assertEquals(50.seconds, options.metricsTimeout)
        assertEquals(60.seconds, options.logsTimeout)

        assertEquals(OtlpProtocol.HTTP_JSON, options.tracesProtocol)
        assertEquals(OtlpProtocol.HTTP_JSON, options.metricsProtocol)
        assertEquals(OtlpProtocol.HTTP_JSON, options.logsProtocol)

        assertEquals(42, options.sampleRate)
        assertTrue(options.debug)
        assertTrue(options.offlineCachingEnabled)
    }

    @Test
    fun options_usesSetValues() {
        val options =
            HoneycombOptions
                .Builder(HoneycombOptionsMapSource(emptyMap<String, Any?>()))
                .setDataset("dataset")
                .setMetricsDataset("metrics")
                .setTracesApiKey("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .setMetricsApiKey("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .setLogsApiKey("cccccccccccccccccccccccccccccccc")
                .setTracesApiEndpoint("http://traces.example.com:1234")
                .setMetricsApiEndpoint("http://metrics.example.com:1234")
                .setLogsApiEndpoint("http://logs.example.com:1234")
                .setSampleRate(42)
                .setDebug(true)
                .setServiceName("service")
                .setServiceVersion("1")
                .setResourceAttributes(mapOf("resource" to "aaa"))
                .setTracesTimeout(40.seconds)
                .setMetricsTimeout(50.seconds)
                .setLogsTimeout(60.seconds)
                .setTracesHeaders(mapOf("header" to "ttt"))
                .setMetricsHeaders(mapOf("header" to "mmm"))
                .setLogsHeaders(mapOf("header" to "lll"))
                .setTracesProtocol(OtlpProtocol.HTTP_JSON)
                .setMetricsProtocol(OtlpProtocol.HTTP_JSON)
                .setLogsProtocol(OtlpProtocol.HTTP_JSON)
                .setOfflineCachingEnabled(true)
                .build()

        assertEquals("service", options.serviceName)
        assertEquals("1", options.serviceVersion)
        assertEquals(
            mapOf(
                "service.name" to "service",
                "service.version" to "1",
                "resource" to "aaa",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )

        assertEquals("http://traces.example.com:1234", options.tracesEndpoint)
        assertEquals("http://metrics.example.com:1234", options.metricsEndpoint)
        assertEquals("http://logs.example.com:1234", options.logsEndpoint)

        assertEquals(
            mapOf(
                "header" to "ttt",
                "x-honeycomb-dataset" to "dataset",
                "x-honeycomb-team" to "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "x-otlp-version" to io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION,
            ),
            options.tracesHeaders,
        )
        assertEquals(
            mapOf(
                "header" to "mmm",
                "x-honeycomb-dataset" to "metrics",
                "x-honeycomb-team" to "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                "x-otlp-version" to io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION,
            ),
            options.metricsHeaders,
        )
        assertEquals(
            mapOf(
                "header" to "lll",
                "x-honeycomb-dataset" to "dataset",
                "x-honeycomb-team" to "cccccccccccccccccccccccccccccccc",
                "x-otlp-version" to io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION,
            ),
            options.logsHeaders,
        )

        assertEquals(40.seconds, options.tracesTimeout)
        assertEquals(50.seconds, options.metricsTimeout)
        assertEquals(60.seconds, options.logsTimeout)

        assertEquals(OtlpProtocol.HTTP_JSON, options.tracesProtocol)
        assertEquals(OtlpProtocol.HTTP_JSON, options.metricsProtocol)
        assertEquals(OtlpProtocol.HTTP_JSON, options.logsProtocol)

        assertEquals(42, options.sampleRate)
        assertTrue(options.debug)
        assertTrue(options.offlineCachingEnabled)
    }

    @Test
    fun options_usesFallbackSetValues() {
        val options =
            HoneycombOptions
                .Builder(HoneycombOptionsMapSource(emptyMap<String, Any?>()))
                .setDataset("dataset")
                .setMetricsDataset("metrics")
                .setApiKey("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .setApiEndpoint("http://api.example.com:1234")
                .setSampleRate(42)
                .setDebug(true)
                .setServiceName("service")
                .setResourceAttributes(mapOf("resource" to "aaa"))
                .setTimeout(30.seconds)
                .setHeaders(mapOf("header" to "hhh"))
                .setProtocol(OtlpProtocol.HTTP_JSON)
                .build()

        assertEquals("service", options.serviceName)
        assertEquals(
            mapOf(
                "service.name" to "service",
                "resource" to "aaa",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )

        assertEquals("http://api.example.com:1234/v1/traces", options.tracesEndpoint)
        assertEquals("http://api.example.com:1234/v1/metrics", options.metricsEndpoint)
        assertEquals("http://api.example.com:1234/v1/logs", options.logsEndpoint)

        assertEquals(
            mapOf(
                "header" to "hhh",
                "x-honeycomb-dataset" to "dataset",
                "x-honeycomb-team" to "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "x-otlp-version" to io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION,
            ),
            options.tracesHeaders,
        )
        assertEquals(
            mapOf(
                "header" to "hhh",
                "x-honeycomb-dataset" to "metrics",
                "x-honeycomb-team" to "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "x-otlp-version" to io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION,
            ),
            options.metricsHeaders,
        )
        assertEquals(
            mapOf(
                "header" to "hhh",
                "x-honeycomb-dataset" to "dataset",
                "x-honeycomb-team" to "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "x-otlp-version" to io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION,
            ),
            options.logsHeaders,
        )

        assertEquals(30.seconds, options.tracesTimeout)
        assertEquals(30.seconds, options.metricsTimeout)
        assertEquals(30.seconds, options.logsTimeout)

        assertEquals(OtlpProtocol.HTTP_JSON, options.tracesProtocol)
        assertEquals(OtlpProtocol.HTTP_JSON, options.metricsProtocol)
        assertEquals(OtlpProtocol.HTTP_JSON, options.logsProtocol)

        assertEquals(42, options.sampleRate)
        assertTrue(options.debug)
    }

    @Test
    fun classicKey_causesDatasetHeader() {
        val key = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val options =
            HoneycombOptions
                .Builder(HoneycombOptionsMapSource(emptyMap<String, Any?>()))
                .setDataset("dataset")
                .setMetricsDataset("metrics")
                .setApiKey(key)
                .build()

        val otlpVersion = io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION
        assertEquals(
            mapOf(
                "x-honeycomb-dataset" to "dataset",
                "x-honeycomb-team" to key,
                "x-otlp-version" to otlpVersion,
            ),
            options.tracesHeaders,
        )
        assertEquals(
            mapOf(
                "x-honeycomb-dataset" to "metrics",
                "x-honeycomb-team" to key,
                "x-otlp-version" to otlpVersion,
            ),
            options.metricsHeaders,
        )
        assertEquals(
            mapOf(
                "x-honeycomb-dataset" to "dataset",
                "x-honeycomb-team" to key,
                "x-otlp-version" to otlpVersion,
            ),
            options.logsHeaders,
        )
    }

    @Test
    fun ingestClassicKey_causesDatasetHeader() {
        val key = "hcaic_7890123456789012345678901234567890123456789012345678901234"
        val options =
            HoneycombOptions
                .Builder(HoneycombOptionsMapSource(emptyMap<String, Any?>()))
                .setDataset("dataset")
                .setMetricsDataset("metrics")
                .setApiKey(key)
                .build()

        val otlpVersion = io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION
        assertEquals(
            mapOf(
                "x-honeycomb-dataset" to "dataset",
                "x-honeycomb-team" to key,
                "x-otlp-version" to otlpVersion,
            ),
            options.tracesHeaders,
        )
        assertEquals(
            mapOf(
                "x-honeycomb-dataset" to "metrics",
                "x-honeycomb-team" to key,
                "x-otlp-version" to otlpVersion,
            ),
            options.metricsHeaders,
        )
        assertEquals(
            mapOf(
                "x-honeycomb-dataset" to "dataset",
                "x-honeycomb-team" to key,
                "x-otlp-version" to otlpVersion,
            ),
            options.logsHeaders,
        )
    }

    @Test
    fun newKey_precludesDatasetHeader() {
        val key = "not_classic"
        val options =
            HoneycombOptions
                .Builder(HoneycombOptionsMapSource(emptyMap<String, Any?>()))
                .setDataset("dataset")
                .setMetricsDataset("metrics")
                .setApiKey(key)
                .build()

        val otlpVersion = io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION
        assertEquals(
            mapOf(
                "x-honeycomb-team" to key,
                "x-otlp-version" to otlpVersion,
            ),
            options.tracesHeaders,
        )
        assertEquals(
            mapOf(
                "x-honeycomb-dataset" to "metrics",
                "x-honeycomb-team" to key,
                "x-otlp-version" to otlpVersion,
            ),
            options.metricsHeaders,
        )
        assertEquals(
            mapOf(
                "x-honeycomb-team" to key,
                "x-otlp-version" to otlpVersion,
            ),
            options.logsHeaders,
        )
    }

    @Test
    fun header_isParsedCorrectly() {
        val source = HoneycombOptionsMapSource(mapOf("data" to "foo=bar,baz=123%20456"))
        val dict = source.getKeyValueList("data")
        assertEquals(
            mapOf(
                "foo" to "bar",
                "baz" to "123 456",
            ),
            dict,
        )
    }

    @Test
    fun options_headersAreMerged() {
        val data =
            mapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
                "OTEL_EXPORTER_OTLP_HEADERS" to "foo=bar,baz=qux",
                "OTEL_EXPORTER_OTLP_TRACES_HEADERS" to "foo=bar2,merged=yes",
            )
        val options = HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()
        assertEquals(
            mapOf(
                "baz" to "qux",
                "foo" to "bar2",
                "merged" to "yes",
                "x-honeycomb-team" to "key",
                "x-otlp-version" to io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION,
            ),
            options.tracesHeaders,
        )
    }

    @Test
    fun options_hasServiceNameDefault() {
        val data = mapOf<String, Any?>("HONEYCOMB_API_KEY" to "key")
        val options = HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()
        assertEquals("unknown_service", options.serviceName)
        assertNull(options.serviceVersion)
        assertEquals(
            mapOf(
                "service.name" to "unknown_service",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )
    }

    @Test
    fun options_serviceFromResourceAttributesSource() {
        val data =
            mapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
                "OTEL_RESOURCE_ATTRIBUTES" to "service.name=resource_name,service.version=1",
            )
        val options = HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()
        assertEquals("resource_name", options.serviceName)
        assertEquals("1", options.serviceVersion)
        assertEquals(
            mapOf(
                "service.name" to "resource_name",
                "service.version" to "1",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )
    }

    @Test
    fun options_individualSourceVariablesTakePrecedence() {
        val data =
            mapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
                "OTEL_SERVICE_NAME" to "service_name",
                "OTEL_SERVICE_VERSION" to "2",
                "OTEL_RESOURCE_ATTRIBUTES" to "service.name=resource_name,service.version=1",
            )
        val options = HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()
        assertEquals("service_name", options.serviceName)
        assertEquals("2", options.serviceVersion)
        assertEquals(
            mapOf(
                "service.name" to "service_name",
                "service.version" to "2",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )
    }

    @Test
    fun options_resourceAttributeSetterTakesPrecedenceOverResourceSource() {
        val data =
            mapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
                "OTEL_RESOURCE_ATTRIBUTES" to "service.name=resource_name,service.version=1,other.attr=1",
            )

        val resourceAttributes = HashMap<String, String>()
        resourceAttributes["service.name"] = "service_name"
        resourceAttributes["service.version"] = "2"

        val options =
            HoneycombOptions
                .Builder(HoneycombOptionsMapSource(data))
                .setResourceAttributes(resourceAttributes)
                .build()

        assertEquals("service_name", options.serviceName)
        assertEquals("2", options.serviceVersion)
        assertEquals(
            mapOf(
                "service.name" to "service_name",
                "service.version" to "2",
                "other.attr" to "1",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )
    }

    @Test
    fun options_resourceAttributeSetterTakesPrecedenceOverSource() {
        val data =
            mapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
                "OTEL_SERVICE_NAME" to "resource_name",
                "OTEL_SERVICE_VERSION" to "1",
            )

        val resourceAttributes = HashMap<String, String>()
        resourceAttributes["service.name"] = "service_name"
        resourceAttributes["service.version"] = "2"

        val options =
            HoneycombOptions
                .Builder(HoneycombOptionsMapSource(data))
                .setResourceAttributes(resourceAttributes)
                .build()

        assertEquals("service_name", options.serviceName)
        assertEquals("2", options.serviceVersion)
        assertEquals(
            mapOf(
                "service.name" to "service_name",
                "service.version" to "2",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )
    }

    @Test
    fun options_individualSettersTakePrecedenceOverSource() {
        val data =
            mapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
                "OTEL_SERVICE_NAME" to "resource_name",
                "OTEL_SERVICE_VERSION" to "1",
            )

        val options =
            HoneycombOptions
                .Builder(HoneycombOptionsMapSource(data))
                .setServiceName("service_name")
                .setServiceVersion("2")
                .build()

        assertEquals("service_name", options.serviceName)
        assertEquals("2", options.serviceVersion)
        assertEquals(
            mapOf(
                "service.name" to "service_name",
                "service.version" to "2",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )
    }

    @Test
    fun options_individualSettersTakePrecedenceOverSources() {
        val data =
            mapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
                "OTEL_RESOURCE_ATTRIBUTES" to "service.name=resource_name,service.version=1",
                "OTEL_SERVICE_NAME" to "service_name",
                "OTEL_SERVICE_VERSION" to "2",
            )

        val options =
            HoneycombOptions
                .Builder(HoneycombOptionsMapSource(data))
                .setServiceName("override_name")
                .setServiceVersion("3")
                .build()

        assertEquals("override_name", options.serviceName)
        assertEquals("3", options.serviceVersion)
        assertEquals(
            mapOf(
                "service.name" to "override_name",
                "service.version" to "3",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )
    }

    @Test
    fun options_individualSettersTakePrecedenceOverResourceAttributeSetter() {
        val data =
            mapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
            )

        val resourceAttributes = HashMap<String, String>()
        resourceAttributes["service.name"] = "resource_name"
        resourceAttributes["service.version"] = "1"

        val options =
            HoneycombOptions
                .Builder(HoneycombOptionsMapSource(data))
                .setResourceAttributes(resourceAttributes)
                .setServiceName("service_name")
                .setServiceVersion("2")
                .build()

        assertEquals("service_name", options.serviceName)
        assertEquals("2", options.serviceVersion)
        assertEquals(
            mapOf(
                "service.name" to "service_name",
                "service.version" to "2",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )
    }

    // very similar to the test above, but note the ordering of the setResourceAttributes() call vs. the setServiceName() call
    @Test
    fun options_individualSettersTakePrecedenceOverResourceAttributeSetter_ordering() {
        val data =
            mapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
            )

        val resourceAttributes = HashMap<String, String>()
        resourceAttributes["service.name"] = "resource_name"
        resourceAttributes["service.version"] = "1"

        val options =
            HoneycombOptions
                .Builder(HoneycombOptionsMapSource(data))
                .setServiceName("service_name")
                .setServiceVersion("2")
                .setResourceAttributes(resourceAttributes)
                .build()

        assertEquals("resource_name", options.serviceName)
        assertEquals("1", options.serviceVersion)
        assertEquals(
            mapOf(
                "service.name" to "resource_name",
                "service.version" to "1",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
                "telemetry.sdk.language" to "android",
                "telemetry.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "telemetry.distro.name" to "io.honeycomb.opentelemetry.android",
            ),
            options.resourceAttributes,
        )
    }

    @Test
    fun options_throwsOnMalformedKeyValueString() {
        val source =
            HoneycombOptionsMapSource(
                mapOf(
                    "HONEYCOMB_API_KEY" to "key",
                    "data" to "foo=bar,baz",
                ),
            )
        assertThrows(HoneycombException::class.java) {
            source.getKeyValueList("data")
        }
    }

    @Test
    fun options_throwsOnUnsupportedProtocol() {
        val source =
            HoneycombOptionsMapSource(
                mapOf<String, Any?>(
                    "HONEYCOMB_API_KEY" to "key",
                    "OTEL_TRACES_EXPORTER" to "not a protocol",
                ),
            )
        assertThrows(HoneycombException::class.java) {
            HoneycombOptions.Builder(source).build()
        }
    }

    @Test
    fun options_throwsOnMissingApiKey() {
        val source =
            HoneycombOptionsMapSource(
                mapOf<String, Any?>(),
            )
        assertThrows(HoneycombException::class.java) {
            HoneycombOptions.Builder(source).build()
        }
    }

    @Test
    fun options_noThrowsOnMissingApiKeyWithCustomEndpoint() {
        val source =
            HoneycombOptionsMapSource(
                mapOf<String, Any?>(
                    "HONEYCOMB_API_ENDPOINT" to "http://example.com:1234",
                ),
            )
        HoneycombOptions.Builder(source).build()
    }
}
