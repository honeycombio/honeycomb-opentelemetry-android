package io.honeycomb.opentelemetry.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources

/**
 * A HoneycombOptionsSource that reads from values resources.
 */
@SuppressLint("DiscouragedApi")
// Suppressed, because while it's not recommended to get an identifier by its string,
// it's the only way for a library to look up an optional value resource.
internal class HoneycombOptionsResourceSource(
    context: Context,
) : HoneycombOptionsSource {
    private val resources: Resources = context.resources
    private val packageName: String = context.packageName

    override fun getString(key: String): String? {
        val id = resources.getIdentifier(key, "string", packageName)
        if (id == 0) {
            return null
        }
        return resources.getString(id).trim().ifEmpty { null }
    }

    override fun getInt(key: String): Int? {
        val id = resources.getIdentifier(key, "integer", packageName)
        if (id == 0) {
            return null
        }
        return resources.getInteger(id)
    }

    override fun getBoolean(key: String): Boolean? {
        val id = resources.getIdentifier(key, "bool", packageName)
        if (id == 0) {
            return null
        }
        return resources.getBoolean(id)
    }
}
