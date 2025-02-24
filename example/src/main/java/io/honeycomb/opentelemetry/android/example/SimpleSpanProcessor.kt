package io.honeycomb.opentelemetry.android.example

import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

class SimpleSpanProcessor: SpanProcessor {
    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        span.setAttribute("app.metadata", "extra metadata")
    }

    override fun isStartRequired(): Boolean {
        return true
    }

    override fun onEnd(span: ReadableSpan) {}

    override fun isEndRequired(): Boolean {
        return false
    }
}
