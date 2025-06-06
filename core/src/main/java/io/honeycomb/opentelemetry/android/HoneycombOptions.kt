package io.honeycomb.opentelemetry.android

import android.content.Context
import android.os.Build
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.semconv.incubating.TelemetryIncubatingAttributes
import java.net.URI
import java.net.URISyntaxException
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
private const val OTEL_SERVICE_VERSION_KEY = "OTEL_SERVICE_VERSION"
private const val OTEL_SERVICE_NAME_DEFAULT = "unknown_service"
private const val OTEL_RESOURCE_ATTRIBUTES_KEY = "OTEL_RESOURCE_ATTRIBUTES"

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

private const val OFFLINE_CACHING_ENABLED = "OFFLINE_CACHING_ENABLED"

/** The protocol for OTLP to use when talking to its backend. */
enum class OtlpProtocol {
    GRPC,
    HTTP_PROTOBUF,
    HTTP_JSON,
    ;

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

private val CLASSIC_KEY_REGEX = Regex("[a-f0-9]*")
private val INGEST_CLASSIC_KEY_REGEX = Regex("hc[a-z]ic_[a-z0-9]*")

/**
 * Returns whether the passed in API key is classic or not.
 */
private fun isClassicKey(key: String?): Boolean {
    if (key == null) {
        return false
    }

    return when (key.length) {
        0 -> false
        32 -> CLASSIC_KEY_REGEX.matches(key)
        64 -> INGEST_CLASSIC_KEY_REGEX.matches(key)
        else -> false
    }
}

private fun isHoneycombEndpoint(endpoint: String): Boolean {
    try {
        val uri = URI(endpoint)
        return uri.host.endsWith(".honeycomb.io")
    } catch (_: URISyntaxException) {
        return false
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
    suffix: String,
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
    apiKey: String?,
    dataset: String?,
    generalHeaders: Map<String, String>,
    signalHeaders: Map<String, String>,
): Map<String, String> {
    val otlpVersion = io.opentelemetry.android.BuildConfig.OTEL_ANDROID_VERSION
    val baseHeaders = mapOf("x-otlp-version" to otlpVersion)
    val signalBaseHeaders = mutableMapOf<String, String>()
    apiKey?.let { signalBaseHeaders["x-honeycomb-team"] = apiKey }
    dataset?.let { signalBaseHeaders["x-honeycomb-dataset"] = it }

    return baseHeaders + generalHeaders + signalBaseHeaders + signalHeaders
}

/** An error when configuring OpenTelemetry for Honeycomb. */
class HoneycombException(
    message: String,
) : Exception(message)

/**
 * The set of options for how to configure Honeycomb.
 *
 * These keys and defaults are defined at:
 * https://github.com/honeycombio/specs/blob/main/specs/otel-sdk-distro.md
 * https://opentelemetry.io/docs/languages/sdk-configuration/general/
 * https://opentelemetry.io/docs/languages/sdk-configuration/otlp-exporter/
 */
data class HoneycombOptions(
    val tracesApiKey: String?,
    val metricsApiKey: String?,
    val logsApiKey: String?,
    val dataset: String?,
    val metricsDataset: String?,
    val tracesEndpoint: String,
    val metricsEndpoint: String,
    val logsEndpoint: String,
    val spanProcessor: SpanProcessor?,
    val logRecordProcessor: LogRecordProcessor?,
    val sampleRate: Int,
    val debug: Boolean,
    val serviceName: String,
    val serviceVersion: String?,
    val resourceAttributes: Map<String, String>,
    val tracesHeaders: Map<String, String>,
    val metricsHeaders: Map<String, String>,
    val logsHeaders: Map<String, String>,
    val tracesTimeout: Duration,
    val metricsTimeout: Duration,
    val logsTimeout: Duration,
    val tracesProtocol: OtlpProtocol,
    val metricsProtocol: OtlpProtocol,
    val logsProtocol: OtlpProtocol,
    val offlineCachingEnabled: Boolean,
) {
    class Builder private constructor() {
        private var serviceVersion: String? = null
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

        private var spanProcessor: SpanProcessor? = null
        private var logRecordProcessor: LogRecordProcessor? = null
        private var sampleRate: Int = 1
        private var debug: Boolean = false

        private var serviceName: String? = null
        private var resourceAttributes: Map<String, String> = mapOf()

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

        private var offlineCachingEnabled: Boolean = false

        constructor(context: Context) : this(HoneycombOptionsResourceSource(context)) {}

        internal constructor(source: HoneycombOptionsSource) : this() {
            configureFromSource(source)
        }

        private fun verifyExporter(
            source: HoneycombOptionsSource,
            key: String,
        ) {
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
            serviceVersion = source.getString(OTEL_SERVICE_VERSION_KEY) ?: serviceVersion
            resourceAttributes = source.getKeyValueList(OTEL_RESOURCE_ATTRIBUTES_KEY)
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
            offlineCachingEnabled = source.getBoolean(OFFLINE_CACHING_ENABLED) ?: offlineCachingEnabled
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

        fun setLogRecordProcessor(logRecordProcessor: LogRecordProcessor): Builder {
            this.logRecordProcessor = logRecordProcessor
            return this
        }

        fun setSpanProcessor(spanProcessor: SpanProcessor): Builder {
            this.spanProcessor = spanProcessor
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

        fun setServiceVersion(appVersion: String): Builder {
            this.serviceVersion = appVersion
            return this
        }

        fun setResourceAttributes(resources: Map<String, String>): Builder {
            val mutable = resourceAttributes.toMutableMap()
            for ((k, v) in resources) {
                mutable[k] = v
            }
            resourceAttributes = mutable

            if (resourceAttributes.containsKey("service.name")) {
                serviceName = resourceAttributes["service.name"]
            }
            if (resourceAttributes.containsKey("service.version")) {
                serviceVersion = resourceAttributes["service.version"]
            }

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

        fun setOfflineCachingEnabled(enabled: Boolean): Builder {
            offlineCachingEnabled = enabled
            return this
        }

        fun build(): HoneycombOptions {
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
            resourceAttributes["service.name"] = serviceName

            val serviceVersion: String? = this.serviceVersion ?: resourceAttributes["service.version"]
            serviceVersion?.let {
                resourceAttributes["service.version"] = it
            }

            // The SDK version is generated from build.gradle.kts.
            resourceAttributes.putIfAbsent(
                "honeycomb.distro.version",
                BuildConfig.HONEYCOMB_DISTRO_VERSION,
            )
            // Use the display version of Android. This is "unknown" when running tests in the JVM.
            resourceAttributes.putIfAbsent(
                "honeycomb.distro.runtime_version",
                Build.VERSION.RELEASE ?: "unknown",
            )

            resourceAttributes.putIfAbsent(
                TelemetryIncubatingAttributes.TELEMETRY_DISTRO_VERSION.key,
                BuildConfig.HONEYCOMB_DISTRO_VERSION,
            )

            resourceAttributes.putIfAbsent(
                TelemetryIncubatingAttributes.TELEMETRY_DISTRO_NAME.key,
                "io.honeycomb.opentelemetry.android",
            )

            // The language is technically Kotlin, but Android apps can be Java or Kotlin or both, so:
            resourceAttributes.putIfAbsent(
                "telemetry.sdk.language",
                "android",
            )

            val tracesEndpoint =
                getHoneycombEndpoint(
                    this.tracesEndpoint,
                    apiEndpoint,
                    tracesProtocol ?: protocol,
                    "v1/traces",
                )
            val metricsEndpoint =
                getHoneycombEndpoint(
                    this.metricsEndpoint,
                    apiEndpoint,
                    metricsProtocol ?: protocol,
                    "v1/metrics",
                )
            val logsEndpoint =
                getHoneycombEndpoint(
                    this.logsEndpoint,
                    apiEndpoint,
                    logsProtocol ?: protocol,
                    "v1/logs",
                )

            val tracesApiKey = this.tracesApiKey ?: this.apiKey
            val metricsApiKey = this.metricsApiKey ?: this.apiKey
            val logsApiKey = this.logsApiKey ?: this.apiKey

            if (isHoneycombEndpoint(tracesEndpoint) && tracesApiKey == null) {
                throw HoneycombException("missing API key: call setApiKey() or setTracesApiKey()")
            }

            if (isHoneycombEndpoint(metricsEndpoint) && metricsApiKey == null) {
                throw HoneycombException("missing API key: call setApiKey() or setMetricsApiKey()")
            }

            if (isHoneycombEndpoint(logsEndpoint) && logsApiKey == null) {
                throw HoneycombException("missing API key: call setApiKey() or setLogsApiKey()")
            }

            val tracesHeaders =
                getHeaders(
                    tracesApiKey,
                    if (isClassicKey(tracesApiKey)) dataset else null,
                    headers,
                    this.tracesHeaders,
                )
            val metricsHeaders =
                getHeaders(
                    metricsApiKey,
                    metricsDataset,
                    headers,
                    this.metricsHeaders,
                )
            val logsHeaders =
                getHeaders(
                    logsApiKey,
                    if (isClassicKey(tracesApiKey)) dataset else null,
                    headers,
                    this.logsHeaders,
                )

            return HoneycombOptions(
                tracesApiKey,
                metricsApiKey,
                logsApiKey,
                dataset,
                metricsDataset,
                tracesEndpoint,
                metricsEndpoint,
                logsEndpoint,
                spanProcessor,
                logRecordProcessor,
                sampleRate,
                debug,
                serviceName,
                serviceVersion,
                resourceAttributes,
                tracesHeaders,
                metricsHeaders,
                logsHeaders,
                tracesTimeout ?: timeout,
                metricsTimeout ?: timeout,
                logsTimeout ?: timeout,
                tracesProtocol ?: protocol,
                metricsProtocol ?: protocol,
                logsProtocol ?: protocol,
                offlineCachingEnabled,
            )
        }
    }

    companion object {
        fun builder(context: Context): HoneycombOptions.Builder {
            return Builder(context)
        }
    }
}
