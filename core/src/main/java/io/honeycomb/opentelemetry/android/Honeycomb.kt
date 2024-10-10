package io.honeycomb.opentelemetry.android

import android.app.Application
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.activity.ActivityLifecycleInstrumentation
import io.opentelemetry.android.instrumentation.activity.startup.AppStartupTimer
import io.opentelemetry.android.instrumentation.anr.AnrDetector
import io.opentelemetry.android.instrumentation.anr.AnrInstrumentation
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.instrumentation.crash.CrashReporterInstrumentation
import io.opentelemetry.android.instrumentation.slowrendering.SlowRenderingInstrumentation
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.logging.otlp.OtlpJsonLoggingSpanExporter
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
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
//
// TODO: Add options for enabling/disabling specific instrumentation.

class Honeycomb {
    companion object {
        /**
         * Automatically configures OpenTelemetryRum based on values stored in the app's resources.
         */
        fun configure(app: Application, options: HoneycombOptions): OpenTelemetryRum {
            val traceExporter = if (options.tracesProtocol == OtlpProtocol.GRPC) {
                OtlpGrpcSpanExporter.builder()
                    .setEndpoint(options.tracesEndpoint)
                    .setTimeout(options.tracesTimeout.toJavaDuration())
                    .setHeaders { options.tracesHeaders }
                    .build()
            } else {
                OtlpHttpSpanExporter.builder()
                    .setEndpoint(options.tracesEndpoint)
                    .setTimeout(options.tracesTimeout.toJavaDuration())
                    .setHeaders { options.tracesHeaders }
                    .build()
            }
            val metricsExporter = if (options.metricsProtocol == OtlpProtocol.GRPC) {
                OtlpGrpcMetricExporter.builder()
                    .setEndpoint(options.metricsEndpoint)
                    .setTimeout(options.metricsTimeout.toJavaDuration())
                    .setHeaders { options.metricsHeaders }
                    .build()
            } else {
                OtlpHttpMetricExporter.builder()
                    .setEndpoint(options.metricsEndpoint)
                    .setTimeout(options.metricsTimeout.toJavaDuration())
                    .setHeaders { options.metricsHeaders }
                    .build()
            }
            val logsExporter = if (options.logsProtocol == OtlpProtocol.GRPC) {
                OtlpGrpcLogRecordExporter.builder()
                    .setEndpoint(options.logsEndpoint)
                    .setTimeout(options.logsTimeout.toJavaDuration())
                    .setHeaders { options.logsHeaders }
                    .build()
            } else {
                OtlpHttpLogRecordExporter.builder()
                    .setEndpoint(options.logsEndpoint)
                    .setTimeout(options.logsTimeout.toJavaDuration())
                    .setHeaders { options.logsHeaders }
                    .build()
            }

            val resource =
                Resource.builder().putAll(createAttributes(options.resourceAttributes)).build()
            val rumConfig = OtelRumConfig()

            val anrInstrumentation = AnrInstrumentation()
            val crashInstrumentation = CrashReporterInstrumentation()
            val lifecycleInstrumentation = ActivityLifecycleInstrumentation()
            val slowRenderingInstrumentation = SlowRenderingInstrumentation()

            // Normally, uncaught exception traces have no name, so add one.
            crashInstrumentation.addAttributesExtractor(
                AttributesExtractor.constant(
                    AttributeKey.stringKey("name"), "UncaughtException"))

            val otelRumBuilder = OpenTelemetryRum.builder(app, rumConfig)
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
                .addInstrumentation(anrInstrumentation)
                .addInstrumentation(crashInstrumentation)
                .addInstrumentation(lifecycleInstrumentation)
                .addInstrumentation(slowRenderingInstrumentation)

            if (options.debug) {
                otelRumBuilder.addSpanExporterCustomizer { OtlpJsonLoggingSpanExporter.create() }
            }

            return otelRumBuilder.build()
        }
    }
}

