package io.honeycomb.opentelemetry.android

import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.contrib.baggage.processor.BaggageSpanProcessor
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.junit.Assert.*
import org.junit.Test

class HoneycombBaggageSpanProcessorUnitTest {
    @Test
    fun processor_addsAttributes() {
        val processor = BaggageSpanProcessor({ key -> key == "keepme" })
        val exporter = InMemorySpanExporter.create()
        val tracerProvider = SdkTracerProvider
            .builder()
            .addSpanProcessor(processor)
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build()

        val tracer = tracerProvider.tracerBuilder("test-scope").build();

        val parent = tracer.spanBuilder("parent").startSpan()

        // create two baggage items, one we will keep and one will
        // be filtered out by the processor
        val newBaggage = Baggage.builder()
            .put("test-key", "test-value")
            .put("keepme", "test-value")
            .build()
        newBaggage.storeInContext(Context.current()).makeCurrent()

        val child = tracer.spanBuilder("child").startSpan()

        child.end()
        parent.end()

        val spans = exporter.finishedSpanItems
        assertEquals(2, spans.size)

        // `first` will throw if the span isn't found.
        val pParent = spans.first { it.name == "parent" }
        assertTrue(pParent.attributes.isEmpty)

        val pChild = spans.first { it.name == "child" }
        assertEquals(1, pChild.attributes.size())
        assertEquals("test-value", pChild.attributes.get(AttributeKey.stringKey("keepme")))
    }
}
