package io.honeycomb.opentelemetry.android.example.nodesugar

import androidx.lifecycle.Lifecycle.State
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs an app without core library desugaring on a real device image. On Android 7 - 11 the
 * upstream slow rendering instrumentation used to throw NoSuchMethodError from onActivityPaused,
 * because it calls Duration.toSeconds(), which only exists on Android 12 and above.
 */
@RunWith(AndroidJUnit4::class)
class NoDesugarSmokeTest {
    @Test
    fun upstreamSlowRenderingIsOnTheClasspath() {
        // Without this the test below could pass without exercising the instrumentation at all.
        assertNotNull(
            Class.forName("io.opentelemetry.android.instrumentation.slowrendering.SlowRenderingInstrumentation"),
        )
    }

    @Test
    fun pausingAnActivityDoesNotCrash() {
        ActivityScenario.launch(NoDesugarActivity::class.java).use { scenario ->
            // Each trip below to STARTED calls onPause, which is where the crash happened.
            scenario.moveToState(State.RESUMED)
            scenario.moveToState(State.STARTED)
            scenario.moveToState(State.RESUMED)
            scenario.moveToState(State.CREATED)
        }
    }
}
