package io.honeycomb.opentelemetry.android

import android.util.Log

private val TAG = "HoneycombDebug"

internal fun configureDebug(options: HoneycombOptions) {
    if (options.debug) {
        Log.d(TAG, "🐝 Honeycomb SDK Debug Mode Enabled 🐝")
        Log.d(TAG, "Honeycomb options: $options")
    }
}
