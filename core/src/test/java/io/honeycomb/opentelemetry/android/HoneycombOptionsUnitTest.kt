package io.honeycomb.opentelemetry.android

import org.junit.Assert.*
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * An implementation of OpenTelemetryConfigSource that reads from a HashMap, so that unit tests can
 * run in the JVM, rather than requiring Android-specific classes.
 */
class HoneycombOptionsMapSource(private val data: Map<String, Any?>) : HoneycombOptionsSource {
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
            ),
            options.resourceAttributes,
        )

        assertEquals("parentbased_always_on", options.tracesSampler)
        assertNull(options.tracesSamplerArg)
        assertEquals("tracecontext,baggage", options.propagators)

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
    }

    @Test
    fun options_handlesEmptyStrings() {
        val data =
            hashMapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
                "OTEL_SERVICE_NAME" to "",
                "OTEL_RESOURCE_ATTRIBUTES" to "",
                "OTEL_TRACES_SAMPLER" to "",
                "OTEL_TRACES_SAMPLER_ARG" to "",
                "OTEL_PROPAGATORS" to "",
                "OTEL_EXPORTER_OTLP_HEADERS" to "",
                "OTEL_EXPORTER_OTLP_TIMEOUT" to null,
                "OTEL_EXPORTER_OTLP_PROTOCOL" to "",
            )
        val options = HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()

        assertEquals("unknown_service", options.serviceName)
        assertEquals(
            mapOf(
                "service.name" to "unknown_service",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
            ),
            options.resourceAttributes,
        )

        assertEquals("parentbased_always_on", options.tracesSampler)
        assertNull(options.tracesSamplerArg)
        assertEquals("tracecontext,baggage", options.propagators)

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
                "OTEL_RESOURCE_ATTRIBUTES" to "resource=aaa",
                "OTEL_TRACES_SAMPLER" to "sampler",
                "OTEL_TRACES_SAMPLER_ARG" to "arg",
                "OTEL_PROPAGATORS" to "propagators",
                "OTEL_EXPORTER_OTLP_HEADERS" to "header=bbb",
                "OTEL_EXPORTER_OTLP_TIMEOUT" to 30000,
                "OTEL_EXPORTER_OTLP_PROTOCOL" to "http/json",
            )
        val options = HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()

        var expectedVersion = System.getenv("CIRCLE_TAG")
        expectedVersion = expectedVersion?.slice(1 until expectedVersion.length) ?: "0.0.0-DEVELOPMENT"

        assertEquals("service", options.serviceName)
        assertEquals(
            mapOf(
                "service.name" to "service",
                "resource" to "aaa",
                "honeycomb.distro.version" to expectedVersion,
                "honeycomb.distro.runtime_version" to "unknown",
            ),
            options.resourceAttributes,
        )

        assertEquals("sampler", options.tracesSampler)
        assertEquals("arg", options.tracesSamplerArg)
        assertEquals("propagators", options.propagators)

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
                "OTEL_RESOURCE_ATTRIBUTES" to "resource=aaa",
                "OTEL_TRACES_SAMPLER" to "sampler",
                "OTEL_TRACES_SAMPLER_ARG" to "arg",
                "OTEL_PROPAGATORS" to "propagators",
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
            )
        val options = HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()

        assertEquals("service", options.serviceName)
        assertEquals(
            mapOf(
                "service.name" to "service",
                "resource" to "aaa",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
            ),
            options.resourceAttributes,
        )

        assertEquals("sampler", options.tracesSampler)
        assertEquals("arg", options.tracesSamplerArg)
        assertEquals("propagators", options.propagators)

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
    }

    @Test
    fun options_usesSetValues() {
        val options =
            HoneycombOptions.Builder(HoneycombOptionsMapSource(emptyMap<String, Any?>()))
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
                .setResourceAttributes(mapOf("resource" to "aaa"))
                .setTracesSampler("sampler")
                .setTracesSamplerArg("arg")
                .setPropagators("propagators")
                .setTracesTimeout(40.seconds)
                .setMetricsTimeout(50.seconds)
                .setLogsTimeout(60.seconds)
                .setTracesHeaders(mapOf("header" to "ttt"))
                .setMetricsHeaders(mapOf("header" to "mmm"))
                .setLogsHeaders(mapOf("header" to "lll"))
                .setTracesProtocol(OtlpProtocol.HTTP_JSON)
                .setMetricsProtocol(OtlpProtocol.HTTP_JSON)
                .setLogsProtocol(OtlpProtocol.HTTP_JSON)
                .build()

        assertEquals("service", options.serviceName)
        assertEquals(
            mapOf(
                "service.name" to "service",
                "resource" to "aaa",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
            ),
            options.resourceAttributes,
        )

        assertEquals("sampler", options.tracesSampler)
        assertEquals("arg", options.tracesSamplerArg)
        assertEquals("propagators", options.propagators)

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
    }

    @Test
    fun options_usesFallbackSetValues() {
        val options =
            HoneycombOptions.Builder(HoneycombOptionsMapSource(emptyMap<String, Any?>()))
                .setDataset("dataset")
                .setMetricsDataset("metrics")
                .setApiKey("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .setApiEndpoint("http://api.example.com:1234")
                .setSampleRate(42)
                .setDebug(true)
                .setServiceName("service")
                .setResourceAttributes(mapOf("resource" to "aaa"))
                .setTracesSampler("sampler")
                .setTracesSamplerArg("arg")
                .setPropagators("propagators")
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
            ),
            options.resourceAttributes,
        )

        assertEquals("sampler", options.tracesSampler)
        assertEquals("arg", options.tracesSamplerArg)
        assertEquals("propagators", options.propagators)

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
            HoneycombOptions.Builder(HoneycombOptionsMapSource(emptyMap<String, Any?>()))
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
            HoneycombOptions.Builder(HoneycombOptionsMapSource(emptyMap<String, Any?>()))
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
            HoneycombOptions.Builder(HoneycombOptionsMapSource(emptyMap<String, Any?>()))
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
    fun options_serviceNameTakesPrecedence() {
        val data =
            mapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
                "OTEL_SERVICE_NAME" to "explicit_name",
                "OTEL_RESOURCE_ATTRIBUTES" to "service.name=resource_name",
            )
        val options = HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()
        assertEquals("explicit_name", options.serviceName)
        assertEquals(
            mapOf(
                "service.name" to "resource_name",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
            ),
            options.resourceAttributes,
        )
    }

    @Test
    fun options_fallsBackToServiceNameFromResourceAttributes() {
        val data =
            mapOf<String, Any?>(
                "HONEYCOMB_API_KEY" to "key",
                "OTEL_RESOURCE_ATTRIBUTES" to "service.name=better",
            )
        val options = HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()
        assertEquals("better", options.serviceName)
        assertEquals(
            mapOf(
                "service.name" to "better",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
            ),
            options.resourceAttributes,
        )
    }

    @Test
    fun options_hasServiceNameDefault() {
        val data = mapOf<String, Any?>("HONEYCOMB_API_KEY" to "key")
        val options = HoneycombOptions.Builder(HoneycombOptionsMapSource(data)).build()
        assertEquals("unknown_service", options.serviceName)
        assertEquals(
            mapOf(
                "service.name" to "unknown_service",
                "honeycomb.distro.version" to BuildConfig.HONEYCOMB_DISTRO_VERSION,
                "honeycomb.distro.runtime_version" to "unknown",
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
}
