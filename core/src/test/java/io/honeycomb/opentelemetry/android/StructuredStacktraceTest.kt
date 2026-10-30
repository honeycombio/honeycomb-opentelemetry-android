package io.honeycomb.opentelemetry.android

import io.honeycomb.opentelemetry.android.shape.parseFrames
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.PrintWriter
import java.io.StringWriter

class StructuredStacktraceTest {
    private fun printed(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

    private fun nativeFrameThrowable(): Throwable {
        val t = RuntimeException("boom", IllegalStateException("cause"))
        // Frames a real device produces that a naive "Class.method(File:line)" pattern gets wrong.
        t.stackTrace =
            arrayOf(
                StackTraceElement("java.lang.Thread", "sleep", null, -2),
                StackTraceElement("com.example.Outer\$Inner", "<init>", "Outer.kt", 12),
                StackTraceElement("com.example.Foo\$\$ExternalSyntheticLambda0", "run", "D8\$\$SyntheticClass", 0),
                StackTraceElement("com.example.Bar", "baz", null, -1),
                StackTraceElement("com.example.Baz", "qux", "Baz.java", 7),
            )
        return t
    }

    @Test
    fun parsesPrintedStackTraceLikeStackTraceElements() {
        val t = nativeFrameThrowable()
        val frames = parseFrames(printed(t))
        // Expected values come from the throwable itself, as Honeycomb.logException reads them.
        // The cause's own frames come after "Caused by:" and must not be included.
        assertEquals(t.stackTrace.map { it.className }, frames.map { it.className })
        assertEquals(t.stackTrace.map { it.methodName }, frames.map { it.methodName })
        assertEquals(t.stackTrace.map { it.lineNumber.toLong() }, frames.map { it.line })
        assertEquals(t.stackTrace.map { it.fileName ?: "" }, frames.map { it.fileName })
    }

    @Test
    fun parsesAnrStyleTraceWithoutAtPrefix() {
        val t = nativeFrameThrowable()
        val anr = t.stackTrace.joinToString("\n") { it.toString() }
        assertEquals(t.stackTrace.map { it.methodName }, parseFrames(anr).map { it.methodName })
    }
}
