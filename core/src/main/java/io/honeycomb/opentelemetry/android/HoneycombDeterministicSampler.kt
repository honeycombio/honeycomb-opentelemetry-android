package io.honeycomb.opentelemetry.android

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.data.LinkData
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.sdk.trace.samplers.SamplingDecision
import io.opentelemetry.sdk.trace.samplers.SamplingResult

class HoneycombDeterministicSampler(private val sampleRate: Int) : Sampler {
    private val inner: Sampler = if (sampleRate < 1) {
        Sampler.alwaysOff()
    } else if (sampleRate == 1) {
        Sampler.alwaysOn()
    } else {
        Sampler.traceIdRatioBased(1.0 / sampleRate)
    }

    override fun shouldSample(context: Context, traceId: String, name: String, spanKind: SpanKind, attributes: Attributes, parentLinks: MutableList<LinkData>): SamplingResult {
        var result = this.inner.shouldSample(context, traceId, name, spanKind, attributes, parentLinks)

        if (result.decision != SamplingDecision.DROP) {
            val attrs = result.attributes.toBuilder().put("SampleRate", sampleRate.toDouble()).build()
            result = SamplingResult.create(result.decision, attrs)
        }

        return result
    }

    override fun getDescription(): String {
        return "DeterministicSampler"
    }
}

//private class HoneycombDecision(val decision: SamplingDecision, val attributes: Attributes) : SamplingResult {
//    override fun getDecision(): SamplingDecision {
//        return decision
//    }
//
//    override fun getAttributes(): Attributes {
//        return attributes
//    }
//}
