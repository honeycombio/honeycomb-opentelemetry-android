package io.honeycomb.opentelemetry.android

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceId
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.data.LinkData
import io.opentelemetry.sdk.trace.samplers.SamplingDecision
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class HoneycombDeterministicSamplerTest(private val args: TestArguments) {
    companion object {
        @JvmStatic
        @Parameters
        fun data(): List<TestArguments> {
            return listOf(
                TestArguments(0, SamplingDecision.DROP),
                TestArguments(1, SamplingDecision.RECORD_AND_SAMPLE),
                TestArguments(10, SamplingDecision.RECORD_AND_SAMPLE),
                TestArguments(100, SamplingDecision.RECORD_AND_SAMPLE),
            )
        }
    }

    @Test
    fun testSampler() {
        // static trace id to ensure the inner traceIdRatio sampler always samples.
        val traceId = TraceId.fromLongs(10L, 10L)
        val context = Context.root()

        val sampler = HoneycombDeterministicSampler(args.rate)
        val result =
            sampler.shouldSample(
                context,
                traceId,
                "test",
                SpanKind.CLIENT,
                Attributes.empty(),
                emptyList<LinkData>().toMutableList(),
            )

        assertEquals(
            "[rate: ${args.rate}] expected: ${args.decision} but was: ${result.decision}",
            args.decision,
            result.decision,
        )
    }
}

data class TestArguments(val rate: Int, val decision: SamplingDecision)
