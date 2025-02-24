package io.honeycomb.opentelemetry.android

import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

class CompositeSpanProcessor: SpanProcessor {
    var spanProcessors: MutableList<SpanProcessor> = ArrayList()

    fun addSpanProcessor(spanProcessor: SpanProcessor) {
        this.spanProcessors.add(spanProcessor)
    }

    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        for (spanProcessor in spanProcessors) {
            spanProcessor.onStart(parentContext, span)
        }
    }

    override fun isStartRequired(): Boolean {
        return spanProcessors.any { it.isStartRequired }
    }

    override fun onEnd(span: ReadableSpan) {
        for (spanProcessor in spanProcessors) {
            spanProcessor.onEnd(span)
        }
    }

    override fun isEndRequired(): Boolean {
        return spanProcessors.any { it.isEndRequired }
    }
}
