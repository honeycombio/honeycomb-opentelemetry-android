package io.honeycomb.opentelemetry.android

import android.content.Context
import android.os.Build
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val HONEYCOMB_API_KEY_KEY = "HONEYCOMB_API_KEY"
private const val HONEYCOMB_TRACES_APIKEY_KEY = "HONEYCOMB_TRACES_APIKEY"
private const val HONEYCOMB_METRICS_APIKEY_KEY = "HONEYCOMB_METRICS_APIKEY"
private const val HONEYCOMB_LOGS_APIKEY_KEY = "HONEYCOMB_LOGS_APIKEY"

private const val HONEYCOMB_DATASET_KEY = "HONEYCOMB_DATASET"
private const val HONEYCOMB_METRICS_DATASET_KEY = "HONEYCOMB_METRICS_DATASET"

private const val HONEYCOMB_API_ENDPOINT_KEY = "HONEYCOMB_API_ENDPOINT"
private const val HONEYCOMB_API_ENDPOINT_DEFAULT = "https://api.honeycomb.io:443"
private const val HONEYCOMB_TRACES_ENDPOINT_KEY = "HONEYCOMB_TRACES_ENDPOINT"
private const val HONEYCOMB_METRICS_ENDPOINT_KEY = "HONEYCOMB_METRICS_ENDPOINT"
private const val HONEYCOMB_LOGS_ENDPOINT_KEY = "HONEYCOMB_LOGS_ENDPOINT"

private const val SAMPLE_RATE_KEY = "SAMPLE_RATE"
private const val DEBUG_KEY = "DEBUG"

private const val OTEL_SERVICE_NAME_KEY = "OTEL_SERVICE_NAME"
private const val OTEL_SERVICE_NAME_DEFAULT = "unknown_service"
private const val OTEL_RESOURCE_ATTRIBUTES_KEY = "OTEL_RESOURCE_ATTRIBUTES"

private const val OTEL_TRACES_SAMPLER_KEY = "OTEL_TRACES_SAMPLER"
private const val OTEL_TRACES_SAMPLER_DEFAULT = "parentbased_always_on"
private const val OTEL_TRACES_SAMPLER_ARG_KEY = "OTEL_TRACES_SAMPLER_ARG"

private const val OTEL_PROPAGATORS_KEY = "OTEL_PROPAGATORS"
private const val OTEL_PROPAGATORS_DEFAULT = "tracecontext,baggage"

private const val OTEL_TRACES_EXPORTER_KEY = "OTEL_TRACES_EXPORTER"
private const val OTEL_METRICS_EXPORTER_KEY = "OTEL_METRICS_EXPORTER"
private const val OTEL_LOGS_EXPORTER_KEY = "OTEL_METRICS_EXPORTER"

private const val OTEL_EXPORTER_OTLP_HEADERS_KEY = "OTEL_EXPORTER_OTLP_HEADERS"
private const val OTEL_EXPORTER_OTLP_TRACES_HEADERS_KEY = "OTEL_EXPORTER_OTLP_TRACES_HEADERS"
private const val OTEL_EXPORTER_OTLP_METRICS_HEADERS_KEY = "OTEL_EXPORTER_OTLP_METRICS_HEADERS"
private const val OTEL_EXPORTER_OTLP_LOGS_HEADERS_KEY = "OTEL_EXPORTER_OTLP_LOGS_HEADERS"

private const val OTEL_EXPORTER_OTLP_TIMEOUT_KEY = "OTEL_EXPORTER_OTLP_TIMEOUT"
private const val OTEL_EXPORTER_OTLP_TRACES_TIMEOUT_KEY = "OTEL_EXPORTER_OTLP_TRACES_TIMEOUT"
private const val OTEL_EXPORTER_OTLP_METRICS_TIMEOUT_KEY = "OTEL_EXPORTER_OTLP_METRICS_TIMEOUT"
private const val OTEL_EXPORTER_OTLP_LOGS_TIMEOUT_KEY = "OTEL_EXPORTER_OTLP_LOGS_TIMEOUT"

private const val OTEL_EXPORTER_OTLP_PROTOCOL_KEY = "OTEL_EXPORTER_OTLP_PROTOCOL"
private const val OTEL_EXPORTER_OTLP_TRACES_PROTOCOL_KEY = "OTEL_EXPORTER_OTLP_TRACES_PROTOCOL"
private const val OTEL_EXPORTER_OTLP_METRICS_PROTOCOL_KEY = "OTEL_EXPORTER_OTLP_METRICS_PROTOCOL"
private const val OTEL_EXPORTER_OTLP_LOGS_PROTOCOL_KEY = "OTEL_EXPORTER_OTLP_LOGS_PROTOCOL"

/** The protocol for OTLP to use when talking to its backend. */
enum class OtlpProtocol {
    GRPC,
    HTTP_PROTOBUF,
    HTTP_JSON;

    companion object {
        internal fun parse(s: String): OtlpProtocol? {
            return when (s) {
                "" -> null
                "grpc" -> GRPC
                "http/protobuf" -> HTTP_PROTOBUF
                "http/json" -> HTTP_JSON
                else -> throw IllegalArgumentException("invalid protocol $s")
            }
        }
    }
}

/**
 * Gets the endpoint to use for a particular signal.
 *
 * The logic is this:
 * 1. If HONEYCOMB_signal_ENDPOINT is set, return it.
 * 2. Determine the base url:
 *    a. If HONEYCOMB_API_ENDPOINT is set, that's the base url.
 *    b. Else, use the default as the base url.
 * 3. If the protocol is GRPC, return the base url.
 * 4. If the protocol is HTTP, return the base url with a suffix based on the signal.
 *
 * Note that even though OpenTelemetry defines its own defaults for endpoints, they will never be
 * used, as the standard Honeycomb-specific logic falls back to its own default.
 */
private fun getHoneycombEndpoint(
    endpoint: String?,
    fallback: String,
    protocol: OtlpProtocol,
    suffix: String
): String {
    if (endpoint != null) {
        return endpoint
    }
    if (protocol == OtlpProtocol.GRPC) {
        return fallback
    }
    return if (fallback.endsWith("/")) {
        "$fallback$suffix"
    } else {
        "$fallback/$suffix"
    }
}

/**
 * Gets the headers to use for a particular exporter.
 */
private fun getHeaders(
    apiKey: String,
    dataset: String?,
    generalHeaders: Map<String, String>,
    signalHeaders: Map<String, String>,
): Map<String, String> {

    val otlpVersion = io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION
    val baseHeaders = mapOf("x-otlp-version" to otlpVersion)
    val signalBaseHeaders = mutableMapOf("x-honeycomb-team" to apiKey)
    dataset?.let { signalBaseHeaders["x-honeycomb-dataset"] = it }

    return baseHeaders + generalHeaders + signalBaseHeaders + signalHeaders
}

/** An error when configuring OpenTelemetry for Honeycomb. */
class HoneycombException(message: String) : Exception(message)

/**
 * The set of options for how to configure Honeycomb.
 *
 * These keys and defaults are defined at:
 * https://github.com/honeycombio/specs/blob/main/specs/otel-sdk-distro.md
 * https://opentelemetry.io/docs/languages/sdk-configuration/general/
 * https://opentelemetry.io/docs/languages/sdk-configuration/otlp-exporter/
 */
data class HoneycombOptions(
    val tracesApiKey: String,
    val metricsApiKey: String,
    val logsApiKey: String,
    val dataset: String?,
    val metricsDataset: String?,
    val tracesEndpoint: String,
    val metricsEndpoint: String,
    val logsEndpoint: String,
    val sampleRate: Int,
    val debug: Boolean,

    val serviceName: String,
    val resourceAttributes: Map<String, String>,
    val tracesSampler: String,
    val tracesSamplerArg: String?,
    val propagators: String,

    val tracesHeaders: Map<String, String>,
    val metricsHeaders: Map<String, String>,
    val logsHeaders: Map<String, String>,

    val tracesTimeout: Duration,
    val metricsTimeout: Duration,
    val logsTimeout: Duration,

    val tracesProtocol: OtlpProtocol,
    val metricsProtocol: OtlpProtocol,
    val logsProtocol: OtlpProtocol,
) {
    class Builder private constructor() {
        private var apiKey: String? = null
        private var tracesApiKey: String? = null
        private var metricsApiKey: String? = null
        private var logsApiKey: String? = null

        private var dataset: String? = null
        private var metricsDataset: String? = null

        private var apiEndpoint: String = HONEYCOMB_API_ENDPOINT_DEFAULT
        private var tracesEndpoint: String? = null
        private var metricsEndpoint: String? = null
        private var logsEndpoint: String? = null

        private var sampleRate: Int = 1
        private var debug: Boolean = false

        private var serviceName: String? = null
        private var resourceAttributes: Map<String, String> = mapOf()
        private var tracesSampler: String = OTEL_TRACES_SAMPLER_DEFAULT
        private var tracesSamplerArg: String? = null
        private var propagators: String = OTEL_PROPAGATORS_DEFAULT

        private var headers: Map<String, String> = mapOf()
        private var tracesHeaders: Map<String, String> = mapOf()
        private var metricsHeaders: Map<String, String> = mapOf()
        private var logsHeaders: Map<String, String> = mapOf()

        private var timeout: Duration = 10.seconds
        private var tracesTimeout: Duration? = null
        private var metricsTimeout: Duration? = null
        private var logsTimeout: Duration? = null

        private var protocol: OtlpProtocol = OtlpProtocol.HTTP_PROTOBUF
        private var tracesProtocol: OtlpProtocol? = null
        private var metricsProtocol: OtlpProtocol? = null
        private var logsProtocol: OtlpProtocol? = null

        constructor(context: Context) : this(HoneycombOptionsResourceSource(context)) {}

        internal constructor(source: HoneycombOptionsSource) : this() {
            configureFromSource(source)
        }

        private fun verifyExporter(source: HoneycombOptionsSource, key: String) {
            val exporter = source.getString(key)?.lowercase() ?: "otlp"
            if (exporter != "otlp") {
                throw HoneycombException("unsupported exporter $exporter for $key")
            }
        }

        private fun configureFromSource(source: HoneycombOptionsSource) {
            // Make sure the exporters aren't set to anything other than OTLP.
            verifyExporter(source, OTEL_TRACES_EXPORTER_KEY)
            verifyExporter(source, OTEL_METRICS_EXPORTER_KEY)
            verifyExporter(source, OTEL_LOGS_EXPORTER_KEY)

            apiKey = source.getString(HONEYCOMB_API_KEY_KEY)
            tracesApiKey = source.getString(HONEYCOMB_TRACES_APIKEY_KEY)
            metricsApiKey = source.getString(HONEYCOMB_METRICS_APIKEY_KEY)
            logsApiKey = source.getString(HONEYCOMB_LOGS_APIKEY_KEY)
            dataset = source.getString(HONEYCOMB_DATASET_KEY)
            metricsDataset = source.getString(HONEYCOMB_METRICS_DATASET_KEY)
            apiEndpoint = source.getString(HONEYCOMB_API_ENDPOINT_KEY) ?: apiEndpoint
            tracesEndpoint = source.getString(HONEYCOMB_TRACES_ENDPOINT_KEY)
            metricsEndpoint = source.getString(HONEYCOMB_METRICS_ENDPOINT_KEY)
            logsEndpoint = source.getString(HONEYCOMB_LOGS_ENDPOINT_KEY)
            sampleRate = source.getInt(SAMPLE_RATE_KEY) ?: sampleRate
            debug = source.getBoolean(DEBUG_KEY) ?: debug
            serviceName = source.getString(OTEL_SERVICE_NAME_KEY) ?: serviceName
            resourceAttributes = source.getKeyValueList(OTEL_RESOURCE_ATTRIBUTES_KEY)
            tracesSampler = source.getString(OTEL_TRACES_SAMPLER_KEY) ?: tracesSampler
            tracesSamplerArg = source.getString(OTEL_TRACES_SAMPLER_ARG_KEY)
            propagators = source.getString(OTEL_PROPAGATORS_KEY) ?: propagators
            headers = source.getKeyValueList(OTEL_EXPORTER_OTLP_HEADERS_KEY)
            tracesHeaders = source.getKeyValueList(OTEL_EXPORTER_OTLP_TRACES_HEADERS_KEY)
            metricsHeaders = source.getKeyValueList(OTEL_EXPORTER_OTLP_METRICS_HEADERS_KEY)
            logsHeaders = source.getKeyValueList(OTEL_EXPORTER_OTLP_LOGS_HEADERS_KEY)
            timeout = source.getDuration(OTEL_EXPORTER_OTLP_TIMEOUT_KEY) ?: timeout
            tracesTimeout = source.getDuration(OTEL_EXPORTER_OTLP_TRACES_TIMEOUT_KEY)
            metricsTimeout = source.getDuration(OTEL_EXPORTER_OTLP_METRICS_TIMEOUT_KEY)
            logsTimeout = source.getDuration(OTEL_EXPORTER_OTLP_LOGS_TIMEOUT_KEY)
            protocol = source.getOtlpProtocol(OTEL_EXPORTER_OTLP_PROTOCOL_KEY) ?: protocol
            tracesProtocol = source.getOtlpProtocol(OTEL_EXPORTER_OTLP_TRACES_PROTOCOL_KEY)
            metricsProtocol = source.getOtlpProtocol(OTEL_EXPORTER_OTLP_METRICS_PROTOCOL_KEY)
            logsProtocol = source.getOtlpProtocol(OTEL_EXPORTER_OTLP_LOGS_PROTOCOL_KEY)
        }

        fun setApiKey(apiKey: String): Builder {
            this.apiKey = apiKey
            return this
        }

        fun setTracesApiKey(apiKey: String): Builder {
            tracesApiKey = apiKey
            return this
        }

        fun setMetricsApiKey(apiKey: String): Builder {
            metricsApiKey = apiKey
            return this
        }

        fun setLogsApiKey(apiKey: String): Builder {
            logsApiKey = apiKey
            return this
        }

        fun setDataset(dataset: String): Builder {
            this.dataset = dataset
            return this
        }

        fun setMetricsDataset(dataset: String): Builder {
            metricsDataset = dataset
            return this
        }

        fun setApiEndpoint(endpoint: String): Builder {
            apiEndpoint = endpoint
            return this
        }

        fun setTracesApiEndpoint(endpoint: String): Builder {
            tracesEndpoint = endpoint
            return this
        }

        fun setMetricsApiEndpoint(endpoint: String): Builder {
            metricsEndpoint = endpoint
            return this
        }

        fun setLogsApiEndpoint(endpoint: String): Builder {
            logsEndpoint = endpoint
            return this
        }

        fun setSampleRate(sampleRate: Int): Builder {
            this.sampleRate = sampleRate
            return this
        }

        fun setDebug(debug: Boolean): Builder {
            this.debug = debug
            return this
        }

        fun setServiceName(serviceName: String): Builder {
            this.serviceName = serviceName
            return this
        }

        fun setResourceAttributes(resources: Map<String, String>): Builder {
            resourceAttributes = resources
            return this
        }

        fun setTracesSampler(sampler: String): Builder {
            tracesSampler = sampler
            return this
        }

        fun setTracesSamplerArg(arg: String?): Builder {
            tracesSamplerArg = arg
            return this
        }

        fun setPropagators(propagators: String): Builder {
            this.propagators = propagators
            return this
        }

        fun setHeaders(headers: Map<String, String>): Builder {
            this.headers = headers
            return this
        }

        fun setTracesHeaders(headers: Map<String, String>): Builder {
            tracesHeaders = headers
            return this
        }

        fun setMetricsHeaders(headers: Map<String, String>): Builder {
            metricsHeaders = headers
            return this
        }

        fun setLogsHeaders(headers: Map<String, String>): Builder {
            logsHeaders = headers
            return this
        }

        fun setTimeout(timeout: Duration): Builder {
            this.timeout = timeout
            return this
        }

        fun setTracesTimeout(timeout: Duration): Builder {
            tracesTimeout = timeout
            return this
        }

        fun setMetricsTimeout(timeout: Duration): Builder {
            metricsTimeout = timeout
            return this
        }

        fun setLogsTimeout(timeout: Duration): Builder {
            logsTimeout = timeout
            return this
        }

        fun setProtocol(protocol: OtlpProtocol): Builder {
            this.protocol = protocol
            return this
        }

        fun setTracesProtocol(protocol: OtlpProtocol): Builder {
            tracesProtocol = protocol
            return this
        }

        fun setMetricsProtocol(protocol: OtlpProtocol): Builder {
            metricsProtocol = protocol
            return this
        }

        fun setLogsProtocol(protocol: OtlpProtocol): Builder {
            logsProtocol = protocol
            return this
        }

        fun build(): HoneycombOptions {
            // If any API key isn't set, consider it a fatal error.
            val defaultApiKey: () -> String = { ->
                if (apiKey == null) {
                    throw HoneycombException("missing API key: call setApiKey()")
                }
                apiKey as String
            }

            // Collect the non-exporter-specific values.
            val resourceAttributes = resourceAttributes.toMutableMap()
            // Any explicit service name overrides the one in the resource attributes.
            val serviceName: String =
                this.serviceName
                    ?: resourceAttributes["service.name"]
                    ?: OTEL_SERVICE_NAME_DEFAULT

            /*
             * Add automatic entries to resource attributes. According to the Honeycomb spec,
             * resource attributes should never be overwritten by automatic values. So, if there are
             * two different service names set, this will use the resource attributes version.
             */
            // Make sure the service name is in the resource attributes.
            resourceAttributes.putIfAbsent("service.name", serviceName)
            // The SDK version is generated from build.gradle.kts.
            resourceAttributes.putIfAbsent(
                "honeycomb.distro.version",
                BuildConfig.HONEYCOMB_DISTRO_VERSION
            )
            // Use the display version of Android. This is "unknown" when running tests in the JVM.
            resourceAttributes.putIfAbsent(
                "honeycomb.distro.runtime_version",
                Build.VERSION.RELEASE ?: "unknown"
            )

            val tracesHeaders =
                getHeaders(
                    tracesApiKey ?: defaultApiKey(),
                    dataset,
                    headers,
                    this.tracesHeaders,
                )
            val metricsHeaders =
                getHeaders(
                    metricsApiKey ?: defaultApiKey(),
                    metricsDataset,
                    headers,
                    this.metricsHeaders,
                )
            val logsHeaders =
                getHeaders(
                    logsApiKey ?: defaultApiKey(),
                    dataset,
                    headers,
                    this.logsHeaders,
                )

            val tracesEndpoint = getHoneycombEndpoint(
                this.tracesEndpoint,
                apiEndpoint,
                tracesProtocol ?: protocol,
                "v1/traces"
            )
            val metricsEndpoint = getHoneycombEndpoint(
                this.metricsEndpoint,
                apiEndpoint,
                metricsProtocol ?: protocol,
                "v1/metrics"
            )
            val logsEndpoint = getHoneycombEndpoint(
                this.logsEndpoint,
                apiEndpoint,
                logsProtocol ?: protocol,
                "v1/logs"
            )

            return HoneycombOptions(
                tracesApiKey ?: defaultApiKey(),
                metricsApiKey ?: defaultApiKey(),
                logsApiKey ?: defaultApiKey(),
                dataset,
                metricsDataset,
                tracesEndpoint,
                metricsEndpoint,
                logsEndpoint,
                sampleRate,
                debug,
                serviceName,
                resourceAttributes,
                tracesSampler,
                tracesSamplerArg,
                propagators,
                tracesHeaders,
                metricsHeaders,
                logsHeaders,
                tracesTimeout ?: timeout,
                metricsTimeout ?: timeout,
                logsTimeout ?: timeout,
                tracesProtocol ?: protocol,
                metricsProtocol ?: protocol,
                logsProtocol ?: protocol,
            )
        }
    }

    companion object {
        fun builder(context: Context): HoneycombOptions.Builder {
            return Builder(context)
        }
    }
}
