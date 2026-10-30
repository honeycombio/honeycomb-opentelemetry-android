package io.honeycomb.opentelemetry.android.shape

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import io.opentelemetry.android.session.SessionProvider
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * A stand-in for upstream's SessionManager, which is internal to android-agent and so cannot be
 * attached to the builder this SDK uses. It exists only to show what has to be re-implemented
 * until upstream exposes it: a session id that rotates after [maxLifetimeMs] or after the app
 * has been in the background for [inactivityMs]. It emits no session.start / session.end events.
 */
class HoneycombSessionProvider(
    app: Application,
    private val maxLifetimeMs: Long = TimeUnit.HOURS.toMillis(4),
    private val inactivityMs: Long = TimeUnit.MINUTES.toMillis(15),
    private val now: () -> Long = SystemClock::elapsedRealtime,
) : SessionProvider,
    Application.ActivityLifecycleCallbacks {
    private var id = UUID.randomUUID().toString().replace("-", "")
    private var startedAt = now()
    private var backgroundedAt: Long? = null
    private var started = 0

    init {
        app.registerActivityLifecycleCallbacks(this)
    }

    @Synchronized
    override fun getSessionId(): String {
        if (now() - startedAt >= maxLifetimeMs) rotate()
        return id
    }

    private fun rotate() {
        id = UUID.randomUUID().toString().replace("-", "")
        startedAt = now()
    }

    @Synchronized
    override fun onActivityStarted(activity: Activity) {
        if (started++ == 0) {
            backgroundedAt?.let { if (now() - it >= inactivityMs) rotate() }
            backgroundedAt = null
        }
    }

    @Synchronized
    override fun onActivityStopped(activity: Activity) {
        if (--started == 0) backgroundedAt = now()
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
