package io.honeycomb.opentelemetry.android

import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.baggage.BaggageEntry
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

class HoneycombBaggageSpanProcessor(val filter: (String, BaggageEntry) -> Boolean): SpanProcessor {
    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        Baggage.current().forEach { key, entry ->
            if (filter(key, entry)) {
                span.setAttribute(key, entry.value)
            }
        }
    }

    override fun isStartRequired(): Boolean {
        return true
    }

    override fun onEnd(span: ReadableSpan) {}

    override fun isEndRequired(): Boolean {
        return false
    }
}
