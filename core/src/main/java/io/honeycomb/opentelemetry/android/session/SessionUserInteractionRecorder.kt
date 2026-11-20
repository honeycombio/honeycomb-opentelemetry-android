/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

// Copied from opentelemetry-android v1.8.1, where it is internal to the agent module:
// https://github.com/open-telemetry/opentelemetry-android/blob/v1.8.1/android-agent/src/main/kotlin/io/opentelemetry/android/agent/session/SessionUserInteractionRecorder.kt
// Keep it in step with upstream until upstream exposes a session provider we can use.

package io.honeycomb.opentelemetry.android.session

/** Records user interaction after checking whether the current session has expired. */
internal fun interface SessionUserInteractionRecorder {
    fun recordUserInteraction()
}
