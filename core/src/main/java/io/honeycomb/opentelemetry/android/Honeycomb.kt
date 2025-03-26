package io.honeycomb.opentelemetry.android

import android.app.Application
import android.os.Build
import android.provider.Settings.Secure
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.OpenTelemetryRumBuilder
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfiguration
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.contrib.baggage.processor.BaggageSpanProcessor
import io.opentelemetry.exporter.logging.otlp.OtlpJsonLoggingMetricExporter
import io.opentelemetry.exporter.logging.otlp.OtlpJsonLoggingSpanExporter
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.export.SpanExporter
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_ID
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MANUFACTURER
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MODEL_NAME
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER
import io.opentelemetry.semconv.incubating.EventIncubatingAttributes.EVENT_NAME
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_ID
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_NAME
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.time.toJavaDuration

private const val CRASH_INSTRUMENTATION_NAME = "io.honeycomb.crash"

/** Creates an Attributes object from a String->String Map. */
private fun createAttributes(dict: Map<String, String>): Attributes {
    val builder = Attributes.builder()
    for (entry in dict) {
        builder.put(entry.key, entry.value)
    }
    return builder.build()
}

private fun getDeviceAttributes(): Attributes {
    val builder = Attributes.builder()

    builder.put(DEVICE_ID, Secure.ANDROID_ID)
    builder.put(DEVICE_MODEL_NAME, Build.MODEL)
    builder.put(DEVICE_MANUFACTURER, Build.MANUFACTURER)
    builder.put(DEVICE_MODEL_IDENTIFIER, Build.ID)

    return builder.build()
}

class Honeycomb {
    companion object {
        /**
         * Automatically configures OpenTelemetryRum based on values stored in the app's resources.
         */
        fun configure(
            app: Application,
            options: HoneycombOptions,
        ): OpenTelemetryRum {
            if (options.debug) {
                configureDebug(options)
            }

            val traceExporter = buildSpanExporter(options)
            val metricsExporter = buildMetricsExporter(options)
            val logsExporter =
                if (options.logsProtocol == OtlpProtocol.GRPC) {
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
                Resource.getDefault().toBuilder()
                    .putAll(createAttributes(options.resourceAttributes))
                    .putAll(getDeviceAttributes())
                    .build()

            val rumConfig = OtelRumConfig()
            val diskBufferingConfig = DiskBufferingConfiguration.builder().setEnabled(options.offlineCachingEnabled).build()
            rumConfig.setDiskBufferingConfiguration(diskBufferingConfig)

            return OpenTelemetryRumBuilder.create(app, rumConfig)
                .mergeResource(resource)
                .addSpanExporterCustomizer { traceExporter }
                .addTracerProviderCustomizer { builder, _ ->
                    val spanProcessor = CompositeSpanProcessor()
                    spanProcessor.addSpanProcessor(BaggageSpanProcessor.allowAllBaggageKeys())
                    options.spanProcessor?.let {
                        spanProcessor.addSpanProcessor(options.spanProcessor)
                    }
                    builder.addSpanProcessor(spanProcessor)
                    builder.setSampler(HoneycombDeterministicSampler(options.sampleRate))
                }
                .addLogRecordExporterCustomizer { logsExporter }
                .addMeterProviderCustomizer { builder, _ ->
                    builder.setResource(resource)
                    builder.registerMetricReader(
                        PeriodicMetricReader.builder(metricsExporter).build(),
                    )
                }
                .build()
        }

        // This code is adapted from the OpenTelemetry crash auto-instrumentation, and should match
        // the format of the events there.
        fun logException(
            otel: OpenTelemetryRum,
            throwable: Throwable,
            attributes: Attributes? = null,
            thread: Thread? = null,
        ) {
            // TODO: It would be nice to include the common RuntimeDetailsExtractor, in order to
            // augment the event with additional metadata, such as memory usage and battery percentage.
            // However, that might require changing this into an entire separate instrumentation
            // package. So for now, just do this.

            val sdk = otel.openTelemetry as OpenTelemetrySdk
            val loggerProvider = sdk.sdkLoggerProvider
            val logger: Logger = loggerProvider.loggerBuilder(CRASH_INSTRUMENTATION_NAME).build()

            val attributesBuilder: AttributesBuilder =
                Attributes.builder()
                    .put(EXCEPTION_STACKTRACE, stackTraceToString(throwable))
                    .put(EXCEPTION_TYPE, throwable.javaClass.name)

            attributes?.let {
                attributesBuilder.putAll(it)
            }

            throwable.message?.let {
                attributesBuilder.put(EXCEPTION_MESSAGE, it)
            }

            thread?.let {
                attributesBuilder
                    .put(THREAD_ID, it.id)
                    .put(THREAD_NAME, it.name)
            }

            attributesBuilder.put(EVENT_NAME, "device.crash")
            logger.logRecordBuilder()
                .setAllAttributes(attributesBuilder.build())
                .emit()
        }

        private fun stackTraceToString(throwable: Throwable): String {
            val sw = StringWriter(256)
            val pw = PrintWriter(sw)

            throwable.printStackTrace(pw)
            pw.flush()

            return sw.toString()
        }

        private fun buildSpanExporter(options: HoneycombOptions): SpanExporter {
            val traceExporter =
                if (options.tracesProtocol == OtlpProtocol.GRPC) {
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

            if (options.debug) {
                return SpanExporter.composite(traceExporter, OtlpJsonLoggingSpanExporter.create())
            }
            return traceExporter
        }

        private fun buildMetricsExporter(options: HoneycombOptions): MetricExporter {
            val metricsExporter =
                if (options.metricsProtocol == OtlpProtocol.GRPC) {
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

            if (options.debug) {
                return CompositeMetricExporter(metricsExporter, OtlpJsonLoggingMetricExporter.create())
            }
            return metricsExporter
        }
    }

    private class CompositeMetricExporter(vararg val exporters: MetricExporter) : MetricExporter {
        override fun getAggregationTemporality(instrumentType: InstrumentType): AggregationTemporality {
            return try {
                exporters.first().getAggregationTemporality(instrumentType)
            } catch (e: NoSuchElementException) {
                // doesn't really matter
                AggregationTemporality.DELTA
            }
        }

        override fun export(metrics: MutableCollection<MetricData>): CompletableResultCode {
            val codes = ArrayList<CompletableResultCode>()
            for (exporter in exporters) {
                codes.add(exporter.export(metrics))
            }
            return CompletableResultCode.ofAll(codes)
        }

        override fun flush(): CompletableResultCode {
            val codes = ArrayList<CompletableResultCode>()
            for (exporter in exporters) {
                codes.add(exporter.flush())
            }
            return CompletableResultCode.ofAll(codes)
        }

        override fun shutdown(): CompletableResultCode {
            val codes = ArrayList<CompletableResultCode>()
            for (exporter in exporters) {
                codes.add(exporter.shutdown())
            }
            return CompletableResultCode.ofAll(codes)
        }
    }
}
