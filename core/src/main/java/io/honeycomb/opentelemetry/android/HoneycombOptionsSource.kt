package io.honeycomb.opentelemetry.android

import java.net.URLDecoder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A dictionary with keys and values for configuring Honeycomb.
 * Provides getters that enforce type safety.
 */
internal interface HoneycombOptionsSource {
    /** Gets a String value. */
    fun getString(key: String): String?

    /** Gets an Int value. */
    fun getInt(key: String): Int?

    /** Gets a Boolean value. */
    fun getBoolean(key: String): Boolean?

    /** Gets a Duration value, which is represented as an integer (in milliseconds). */
    fun getDuration(key: String): Duration? {
        return getInt(key)?.milliseconds
    }

    /** Gets an OTLP protocol, */
    fun getOtlpProtocol(key: String): OtlpProtocol? {
        return getString(key)?.let { OtlpProtocol.parse(it) }
    }

    /** Gets and parses a comma-delimited list of key-value pairs. */
    fun getKeyValueList(key: String): Map<String, String> {
        return getString(key)?.let { parseKeyValueList(it) }.orEmpty()
    }

    /**
     * Parses a list of key-value pairs, as used in specifying resources and headers.
     * Headers are comma-separated pairs with equals, such as:
     *     key1=value1,key2=value2
     * See the format specified here:
     *     https://opentelemetry.io/docs/specs/otel/resource/sdk/#specifying-resource-information-via-an-environment-variable
     */
    @Suppress("DEPRECATION")
    private fun parseKeyValueList(keyValueString: String): Map<String, String> {
        val result = HashMap<String, String>()
        val parts = keyValueString.split(",").map { it.trim() }
        parts.filter { it.isNotEmpty() }.map {
            val keyAndValue = it.split("=").map { it.trim() }
            if (keyAndValue.size != 2) {
                throw HoneycombException("invalid key-value pair: $it")
            }
            // We use the deprecated version of URLDecoder.decode, because:
            // 1. The current version requires Android minSdk API level 33 or higher.
            // 2. Uri.decode would replace invalid encodings with \uFFFD instead of throwing.
            // 3. Uri.decode is Android-specific, and won't work in unit tests.
            val key = URLDecoder.decode(keyAndValue[0])
            val value = URLDecoder.decode(keyAndValue[1])
            result[key] = value
        }
        return result
    }
}
