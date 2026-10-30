package io.honeycomb.passthrough.harness

import com.google.protobuf.util.JsonFormat
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * A loopback-only OTLP/HTTP (protobuf) sink. Every request is decoded and appended to [out] as
 * one JSON line: {"path": ..., "headers": {...}, "body": <OTLP JSON>}.
 */
class OtlpSink(
    private val out: File,
) : AutoCloseable {
    private val server = MockWebServer()
    private val printer = JsonFormat.printer().omittingInsignificantWhitespace()
    private val lock = Any()

    val endpoint: String get() = server.url("/").toString().trimEnd('/')

    init {
        out.parentFile.mkdirs()
        out.writeText("")
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val raw = request.body?.toByteArray() ?: ByteArray(0)
                    val bytes =
                        if (request.headers["Content-Encoding"] == "gzip") {
                            GZIPInputStream(raw.inputStream()).readBytes()
                        } else {
                            raw
                        }
                    val path = request.url.encodedPath
                    val json =
                        when (path) {
                            "/v1/traces" -> printer.print(ExportTraceServiceRequest.parseFrom(bytes))
                            "/v1/logs" -> printer.print(ExportLogsServiceRequest.parseFrom(bytes))
                            "/v1/metrics" -> printer.print(ExportMetricsServiceRequest.parseFrom(bytes))
                            else -> "null"
                        }
                    val headers =
                        request.headers.joinToString(",", "{", "}") { (k, v) ->
                            "\"${k.lowercase()}\":\"${v.replace("\"", "'")}\""
                        }
                    synchronized(lock) {
                        out.appendText("{\"path\":\"$path\",\"headers\":$headers,\"body\":$json}\n")
                    }
                    return MockResponse(code = 200)
                }
            }
        server.start()
    }

    override fun close() = server.close()
}
