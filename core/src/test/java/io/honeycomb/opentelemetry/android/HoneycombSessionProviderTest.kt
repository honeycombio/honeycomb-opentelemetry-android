package io.honeycomb.opentelemetry.android

import android.app.Activity
import android.app.Application
import io.honeycomb.opentelemetry.android.shape.HoneycombSessionProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class HoneycombSessionProviderTest {
    private var now = 0L
    private val activity = mock(Activity::class.java)
    private val provider =
        HoneycombSessionProvider(
            mock(Application::class.java),
            maxLifetimeMs = 1000,
            inactivityMs = 100,
            now = { now },
        )

    @Test
    fun idIsStableWhileForegroundAndYoungerThanMaxLifetime() {
        provider.onActivityStarted(activity)
        val id = provider.getSessionId()
        assertTrue(id.isNotEmpty())
        now = 999
        assertEquals(id, provider.getSessionId())
    }

    @Test
    fun idRotatesAtMaxLifetime() {
        val id = provider.getSessionId()
        now = 1000
        assertNotEquals(id, provider.getSessionId())
    }

    @Test
    fun idRotatesWhenForegroundedAfterLongBackground() {
        provider.onActivityStarted(activity)
        val id = provider.getSessionId()
        provider.onActivityStopped(activity)
        now = 100
        provider.onActivityStarted(activity)
        assertNotEquals(id, provider.getSessionId())
    }

    @Test
    fun idSurvivesShortBackgroundAndActivityTransitions() {
        val second = mock(Activity::class.java)
        provider.onActivityStarted(activity)
        val id = provider.getSessionId()
        // Activity A -> B: B starts before A stops, so the app never counts as backgrounded.
        now = 500
        provider.onActivityStarted(second)
        provider.onActivityStopped(activity)
        now = 900
        provider.onActivityStopped(second)
        now = 950
        provider.onActivityStarted(second)
        assertEquals(id, provider.getSessionId())
    }
}
