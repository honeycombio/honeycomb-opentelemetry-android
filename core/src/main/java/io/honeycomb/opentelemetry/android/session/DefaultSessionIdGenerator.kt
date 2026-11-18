/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

// Copied from opentelemetry-android v1.8.1, where it is internal to the agent module:
// https://github.com/open-telemetry/opentelemetry-android/blob/v1.8.1/android-agent/src/main/kotlin/io/opentelemetry/android/agent/session/DefaultSessionIdGenerator.kt
// Keep it in step with upstream until upstream exposes a session provider we can use.

package io.honeycomb.opentelemetry.android.session

import io.opentelemetry.api.trace.TraceId
import kotlin.random.Random

internal class DefaultSessionIdGenerator(
    private val random: Random,
) : SessionIdGenerator {
    override fun generateSessionId(): String {
        // The OTel TraceId has exactly the same format as a RUM SessionId, so let's re-use it here,
        // rather than re-inventing the wheel.
        return TraceId.fromLongs(random.nextLong(), random.nextLong())
    }
}
