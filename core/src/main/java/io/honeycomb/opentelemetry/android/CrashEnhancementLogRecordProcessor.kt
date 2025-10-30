package io.honeycomb.opentelemetry.android

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

/**
 * A LogRecordProcessor that automatically enhances crash log records from the OpenTelemetry
 * Android crash instrumentation with structured stacktrace attributes.
 *
 * When the OpenTelemetry crash instrumentation (io.opentelemetry.android:instrumentation-crash)
 * creates log records for crashes, this processor parses the exception.stacktrace attribute
 * and adds structured stacktrace attributes for easier querying and analysis:
 * - exception.structured_stacktrace.classes
 * - exception.structured_stacktrace.methods
 * - exception.structured_stacktrace.lines
 * - exception.structured_stacktrace.source_files
 */
internal class CrashEnhancementLogRecordProcessor : LogRecordProcessor {
    override fun onEmit(
        context: Context,
        logRecord: ReadWriteLogRecord,
    ) {
        // Get the attributes from the log record
        val attributes = logRecord.toLogRecordData().attributes

        // Get the exception stacktrace attribute
        val stacktrace =
            attributes.get(
                AttributeKey.stringKey("exception.stacktrace"),
            )

        if (stacktrace != null) {
            // Check if this is a crash log record from OTel instrumentation
            val instrumentationName = logRecord.instrumentationScopeInfo?.name
            if (instrumentationName != null &&
                (instrumentationName.contains("crash", ignoreCase = true) ||
                instrumentationName == "io.opentelemetry.crash")
            ) {
                // Parse stacktrace and add structured attributes
                val stackFrames = parseStackTrace(stacktrace)
                if (stackFrames.isNotEmpty()) {
                    logRecord.setAttribute(
                        AttributeKey.stringArrayKey("exception.structured_stacktrace.classes"),
                        stackFrames.map { it.className },
                    )
                    logRecord.setAttribute(
                        AttributeKey.stringArrayKey("exception.structured_stacktrace.methods"),
                        stackFrames.map { it.methodName },
                    )
                    logRecord.setAttribute(
                        AttributeKey.longArrayKey("exception.structured_stacktrace.lines"),
                        stackFrames.map { it.lineNumber.toLong() },
                    )
                    logRecord.setAttribute(
                        AttributeKey.stringArrayKey("exception.structured_stacktrace.source_files"),
                        stackFrames.map { it.fileName ?: "Unknown" },
                    )
                }
            }
        }
    }

    /**
     * Parses a Java stacktrace string into structured frame information.
     * Handles standard Java stacktrace format: "at com.example.Class.method(File.java:123)"
     */
    private fun parseStackTrace(stacktrace: String): List<StackFrame> {
        val frames = mutableListOf<StackFrame>()
        val lines = stacktrace.split("\n")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("at ")) {
                try {
                    // Extract: "at com.example.MyClass.myMethod(MyFile.java:42)"
                    val parts = trimmed.substring(3).split("(")
                    if (parts.size == 2) {
                        val classAndMethod = parts[0].trim().split(".")
                        val methodName = classAndMethod.lastOrNull() ?: "unknown"
                        val className = classAndMethod.dropLast(1).joinToString(".")

                        val fileAndLine = parts[1].removeSuffix(")").split(":")
                        val fileName = fileAndLine.getOrNull(0)
                        val lineNumber = fileAndLine.getOrNull(1)?.toIntOrNull() ?: -1

                        frames.add(StackFrame(className, methodName, fileName, lineNumber))
                    }
                } catch (e: Exception) {
                    // Skip malformed lines
                }
            }
        }

        return frames
    }

    private data class StackFrame(
        val className: String,
        val methodName: String,
        val fileName: String?,
        val lineNumber: Int,
    )
}
