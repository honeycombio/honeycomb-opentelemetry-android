/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

// Copied from opentelemetry-android v1.8.1, where it is internal to the agent module:
// https://github.com/open-telemetry/opentelemetry-android/blob/v1.8.1/android-agent/src/main/kotlin/io/opentelemetry/android/agent/session/SessionConfig.kt
// Keep it in step with upstream until upstream exposes a session provider we can use.

package io.honeycomb.opentelemetry.android.session

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal class SessionConfig(
    val backgroundInactivityTimeout: Duration = 15.minutes,
    val maxLifetime: Duration = 4.hours,
    val userInactivityTimeout: Duration? = null,
) {
    companion object {
        @JvmStatic
        fun withDefaults(): SessionConfig = SessionConfig()
    }
}
