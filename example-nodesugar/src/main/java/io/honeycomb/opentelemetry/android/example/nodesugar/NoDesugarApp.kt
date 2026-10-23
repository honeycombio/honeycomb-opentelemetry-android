package io.honeycomb.opentelemetry.android.example.nodesugar

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.widget.TextView
import io.honeycomb.opentelemetry.android.Honeycomb
import io.honeycomb.opentelemetry.android.HoneycombOptions

class NoDesugarApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // The endpoint is unreachable on purpose: this app only checks that it does not crash, and
        // must not send anything to the collector that the :example smoke tests assert on.
        val options =
            HoneycombOptions
                .builder(this)
                .setApiKey("test-key")
                .setApiEndpoint("http://127.0.0.1:1")
                .setServiceName("android-test-nodesugar")
                .build()
        Honeycomb.configure(this, options)
    }
}

class NoDesugarActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "No core library desugaring" })
    }
}
