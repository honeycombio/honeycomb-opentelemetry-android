package io.honeycomb.passthrough.harness

import android.app.Application
import io.honeycomb.opentelemetry.android.Honeycomb
import io.honeycomb.opentelemetry.android.HoneycombOptions
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

const val FAKE_API_KEY = "not-a-real-key"

/** The SDK setups the same scenario is run against. */
fun startSetup(
    setup: String,
    app: Application,
    endpoint: String,
): OpenTelemetryRum =
    when (setup) {
        // Today's SDK: Honeycomb.configure over the upstream builder.
        "current" ->
            Honeycomb.configure(
                app,
                HoneycombOptions
                    .builder(app)
                    .setApiKey(FAKE_API_KEY)
                    .setApiEndpoint(endpoint)
                    .setServiceName("harness")
                    .setServiceVersion("0.0.1")
                    .setSpanProcessor(MetadataSpanProcessor())
                    .setLogRecordProcessor(MetadataLogRecordProcessor())
                    .build(),
            )
        // Upstream only: the stock OpenTelemetryRumInitializer from android-agent. The only
        // concession is the header that carries the (fake) key and the sink address.
        "upstream" ->
            OpenTelemetryRumInitializer.initialize(app) {
                httpExport {
                    baseUrl = endpoint
                    baseHeaders = mapOf("x-honeycomb-team" to FAKE_API_KEY)
                }
                // Upstream defaults to disk buffering, which delays export by at least 30s;
                // Honeycomb.configure defaults to off, so match it to see the data at all.
                diskBuffering { enabled(false) }
            }
        else -> error("unknown setup $setup")
    }

/** The kind of user-supplied processors Honeycomb.configure accepts (see the example app). */
class MetadataSpanProcessor : SpanProcessor {
    override fun onStart(
        parentContext: Context,
        span: ReadWriteSpan,
    ) {
        span.setAttribute(AttributeKey.stringKey("app.metadata"), "extra metadata")
    }

    override fun isStartRequired() = true

    override fun onEnd(span: ReadableSpan) {}

    override fun isEndRequired() = false
}

class MetadataLogRecordProcessor : LogRecordProcessor {
    override fun onEmit(
        context: Context,
        logRecord: ReadWriteLogRecord,
    ) {
        logRecord.setAttribute(AttributeKey.stringKey("app.metadata"), "extra metadata")
    }
}
