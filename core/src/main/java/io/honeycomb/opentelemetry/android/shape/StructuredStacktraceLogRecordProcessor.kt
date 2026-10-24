package io.honeycomb.opentelemetry.android.shape

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE

private val CLASSES = AttributeKey.stringArrayKey("exception.structured_stacktrace.classes")
private val METHODS = AttributeKey.stringArrayKey("exception.structured_stacktrace.methods")
private val LINES = AttributeKey.longArrayKey("exception.structured_stacktrace.lines")
private val SOURCE_FILES = AttributeKey.stringArrayKey("exception.structured_stacktrace.source_files")
private val PROGUARD_UUID = AttributeKey.stringKey("app.debug.proguard_uuid")

/** `at com.example.Foo.bar(Foo.kt:12)` or, for ANR traces, `com.example.Foo.bar(Foo.kt:12)`. */
private val FRAME = Regex("""^\s*(?:at\s+)?(\S+)\.([^.\s(]+)\((.*)\)\s*$""")

internal class Frame(
    val className: String,
    val methodName: String,
    val fileName: String,
    val line: Long,
)

/**
 * Parses the frames of the top-level throwable out of [Throwable.printStackTrace] output (or an
 * ANR trace, which is one `StackTraceElement.toString()` per line). Frames of `Caused by:` and
 * `Suppressed:` blocks are not included, to match what [io.honeycomb.opentelemetry.android.Honeycomb.logException]
 * has always reported.
 */
internal fun parseFrames(stacktrace: String): List<Frame> {
    val frames = ArrayList<Frame>()
    for (line in stacktrace.lineSequence()) {
        val trimmed = line.trim()
        if (trimmed.startsWith("Caused by:") || trimmed.startsWith("Suppressed:")) break
        val match = FRAME.matchEntire(line) ?: continue
        val (cls, method, location) = match.destructured
        val colon = location.lastIndexOf(':')
        val (file, lineNumber) =
            when {
                location == "Native Method" -> "" to -2L
                location == "Unknown Source" -> "" to -1L
                colon > 0 && location.substring(colon + 1).toLongOrNull() != null ->
                    location.substring(0, colon) to location.substring(colon + 1).toLong()
                else -> location to -1L
            }
        frames.add(Frame(cls, method, file, lineNumber))
    }
    return frames
}

/**
 * Adds the `exception.structured_stacktrace.*` arrays (and the ProGuard UUID) to every log
 * record that carries `exception.stacktrace`: upstream's crash and ANR events as well as this
 * SDK's own `logException`. Upstream only reports the string form.
 */
class StructuredStacktraceLogRecordProcessor(
    private val proguardUuid: () -> String?,
) : LogRecordProcessor {
    override fun onEmit(
        context: Context,
        logRecord: ReadWriteLogRecord,
    ) {
        val stacktrace = logRecord.getAttribute(EXCEPTION_STACKTRACE) ?: return
        if (logRecord.getAttribute(CLASSES) == null) {
            val frames = parseFrames(stacktrace)
            if (frames.isNotEmpty()) {
                logRecord.setAttribute(CLASSES, frames.map { it.className })
                logRecord.setAttribute(METHODS, frames.map { it.methodName })
                logRecord.setAttribute(LINES, frames.map { it.line })
                logRecord.setAttribute(SOURCE_FILES, frames.map { it.fileName })
            }
        }
        if (logRecord.getAttribute(PROGUARD_UUID) == null) {
            proguardUuid()?.let { logRecord.setAttribute(PROGUARD_UUID, it) }
        }
    }
}
