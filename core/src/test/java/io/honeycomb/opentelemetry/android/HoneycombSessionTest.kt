package io.honeycomb.opentelemetry.android

import android.app.Application
import io.honeycomb.opentelemetry.android.session.SessionConfig
import io.honeycomb.opentelemetry.android.session.SessionIdTimeoutHandler
import io.honeycomb.opentelemetry.android.session.SessionManager
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.common.Clock
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.minutes

private val SESSION_ID = AttributeKey.stringKey("session.id")

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HoneycombSessionTest {
    @Test
    fun spansAndLogsShareANonEmptySessionId() {
        val app = RuntimeEnvironment.getApplication() as Application
        val spans = InMemorySpanExporter.create()
        val logs = InMemoryLogRecordExporter.create()
        val options =
            HoneycombOptions
                .builder(app)
                .setApiKey("test-key")
                .setServiceName("session-test")
                .setSpanProcessor(SimpleSpanProcessor.create(spans))
                .setLogRecordProcessor(SimpleLogRecordProcessor.create(logs))
                .build()

        val otel = Honeycomb.configure(app, options).openTelemetry
        val tracer = otel.getTracer("session-test")
        tracer.spanBuilder("first").startSpan().end()
        tracer.spanBuilder("second").startSpan().end()
        otel.logsBridge
            .get("session-test")
            .logRecordBuilder()
            .setBody("log")
            .emit()

        val first =
            spans.finishedSpanItems
                .single { it.name == "first" }
                .attributes
                .get(SESSION_ID)
        val second =
            spans.finishedSpanItems
                .single { it.name == "second" }
                .attributes
                .get(SESSION_ID)
        val log =
            logs.finishedLogRecordItems
                .single { it.bodyValue?.asString() == "log" }
                .attributes
                .get(SESSION_ID)

        assertTrue("span has no session.id: $first", !first.isNullOrEmpty())
        assertEquals(first, second)
        assertEquals(first, log)
    }

    @Test
    fun sessionIdChangesAfterTheBackgroundTimeout() {
        val clock = FakeClock()
        val config = SessionConfig(backgroundInactivityTimeout = 15.minutes)
        val timeoutHandler = SessionIdTimeoutHandler(config, clock)
        val sessions = SessionManager.create(timeoutHandler, config, clock)

        val before = sessions.getSessionId()
        timeoutHandler.onApplicationBackgrounded()
        clock.advance(14.minutes.inWholeNanoseconds)
        assertEquals(before, sessions.getSessionId())

        clock.advance(2.minutes.inWholeNanoseconds)
        timeoutHandler.onApplicationForegrounded()
        val after = sessions.getSessionId()
        assertTrue(after.isNotEmpty())
        assertNotEquals(before, after)
    }

    private class FakeClock : Clock {
        private var nanos = 1_000_000_000L

        fun advance(by: Long) {
            nanos += by
        }

        override fun now(): Long = nanos

        override fun nanoTime(): Long = nanos
    }
}
