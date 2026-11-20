/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

// Copied from opentelemetry-android v1.8.1, where it is internal to the agent module:
// https://github.com/open-telemetry/opentelemetry-android/blob/v1.8.1/android-agent/src/main/kotlin/io/opentelemetry/android/agent/session/InMemorySessionStorage.kt
// Keep it in step with upstream until upstream exposes a session provider we can use.
// Upstream's @Incubating opt-in markers are removed, as they live in the agent module.

package io.honeycomb.opentelemetry.android.session

import io.opentelemetry.android.session.Session

internal class InMemorySessionStorage(
    @Volatile var session: Session = invalidSession,
) : SessionStorage {
    override fun get(): Session = session

    override fun save(newSession: Session) {
        this.session = newSession
    }
}
