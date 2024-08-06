package io.honeycomb.opentelemetry.android

import android.app.Application
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import kotlin.time.toJavaDuration

/** Creates an Attributes object from a String->String Map. */
private fun createAttributes(dict: Map<String, String>): Attributes {
    val builder = Attributes.builder()
    for (entry in dict) {
        builder.put(entry.key, entry.value)
    }
    return builder.build()
}

// TODO: Implement the following features.
//       https://github.com/honeycombio/specs/blob/main/specs/otel-sdk-distro.md
//
// * DeterministicSampler.
// * BaggageSpanProcessor.
// * BatchSpanProcessor.
// * LoggingSpanExporter.
// * LoggingMetricExporter.
// * Debug logging.

class Honeycomb {
    companion object {
        /**
         * Automatically configures OpenTelemetryRum based on values stored in the app's resources.
         */
        fun configure(app: Application) {
            val config = HoneycombOptions.Builder(app).build()

            val traceExporter = if (config.tracesProtocol == OtlpProtocol.GRPC) {
                OtlpGrpcSpanExporter.builder()
                    .setEndpoint(config.tracesEndpoint)
                    .setTimeout(config.tracesTimeout.toJavaDuration())
                    .setHeaders { config.tracesHeaders }
                    .build()
            } else {
                OtlpHttpSpanExporter.builder()
                    .setEndpoint(config.tracesEndpoint)
                    .setTimeout(config.tracesTimeout.toJavaDuration())
                    .setHeaders { config.tracesHeaders }
                    .build()
            }
            val metricsExporter = if (config.metricsProtocol == OtlpProtocol.GRPC) {
                OtlpGrpcMetricExporter.builder()
                    .setEndpoint(config.metricsEndpoint)
                    .setTimeout(config.metricsTimeout.toJavaDuration())
                    .setHeaders { config.metricsHeaders }
                    .build()
            } else {
                OtlpHttpMetricExporter.builder()
                    .setEndpoint(config.metricsEndpoint)
                    .setTimeout(config.metricsTimeout.toJavaDuration())
                    .setHeaders { config.metricsHeaders }
                    .build()
            }
            val logsExporter = if (config.logsProtocol == OtlpProtocol.GRPC) {
                OtlpGrpcLogRecordExporter.builder()
                    .setEndpoint(config.logsEndpoint)
                    .setTimeout(config.logsTimeout.toJavaDuration())
                    .setHeaders { config.logsHeaders }
                    .build()
            } else {
                OtlpHttpLogRecordExporter.builder()
                    .setEndpoint(config.logsEndpoint)
                    .setTimeout(config.logsTimeout.toJavaDuration())
                    .setHeaders { config.logsHeaders }
                    .build()
            }

            val resource =
                Resource.builder().putAll(createAttributes(config.resourceAttributes)).build()
            val rumConfig = OtelRumConfig()
            val otelRum = OpenTelemetryRum.builder(app, rumConfig)
                .setResource(resource)
                .addSpanExporterCustomizer {
                    traceExporter
                }
                .addMeterProviderCustomizer { builder, _ ->
                    builder.setResource(resource)
                    builder.registerMetricReader(
                        PeriodicMetricReader.builder(metricsExporter).build()
                    )
                }
                .addLoggerProviderCustomizer { builder, _ ->
                    builder.setResource(resource)
                    builder.addLogRecordProcessor(SimpleLogRecordProcessor.create(logsExporter))
                }
                .build()

            GlobalOpenTelemetry.set(otelRum.openTelemetry)
        }
    }
}

