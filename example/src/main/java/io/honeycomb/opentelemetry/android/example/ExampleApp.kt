package io.honeycomb.opentelemetry.android.example

import android.app.Application
import android.os.Handler
import android.os.Looper
import io.honeycomb.opentelemetry.android.Honeycomb
import io.honeycomb.opentelemetry.android.HoneycombOptions
import io.opentelemetry.android.OpenTelemetryRum
import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// An exception that won't crash the app if it isn't caught.
// This is used for testing crash instrumentation.
class SmokeTestException(
    message: String?,
) : Exception(message)

/*
 * In order to test the uncaught exception handler, we need to be able to throw some exceptions and
 * handle them in between when the OpenTelemetry UncaughtExceptionHandler runs and when they would
 * actually crash the apps.
 */
class SmokeTestExceptionHandler : UncaughtExceptionHandler {
    var existingHandler: Thread.UncaughtExceptionHandler? = null

    override fun uncaughtException(
        t: Thread,
        e: Throwable,
    ) {
        if (e !is SmokeTestException) {
            existingHandler?.uncaughtException(t, e)
        }
    }

    fun install() {
        existingHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
}

class ExampleApp : Application() {
    var otelRum: OpenTelemetryRum? = null

    override fun onCreate() {
        super.onCreate()

        SmokeTestExceptionHandler().install()

        // To use this sample app with Honeycomb, change the api key to your own key, and remove
        // the call to setApiEndpoint.
        val options =
            HoneycombOptions
                .builder(this)
                .setApiKey("test-key")
                .setApiEndpoint("http://10.0.2.2:4318")
                .setServiceName("android-test")
                .setServiceVersion("0.0.1")
                .setMetricsDataset("android-test-metrics")
                .setSpanProcessor(SimpleSpanProcessor())
                .setLogRecordProcessor(SimpleLogRecordProcessor())
                .setDebug(true)
                .build()

        otelRum = Honeycomb.configurePassthrough(this, options)
    }

    fun flush() {
        // On upstream 1.8.0, openTelemetry is no longer an OpenTelemetrySdk, so it cannot be cast
        // to flush the providers; shutting the RUM instance down flushes them. shutdown() removes a
        // lifecycle observer, so it must run on the main thread (CI caught this when called from
        // the instrumentation thread).
        val done = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post {
            otelRum?.shutdown()
            done.countDown()
        }
        done.await(5, TimeUnit.SECONDS)
        this.otelRum = null
        // Theoretically, flushing and shutting down should be sufficient.
        // But empirically, if we don't sleep, sometimes the traces don't show up.
        Thread.sleep(500)
    }
}
