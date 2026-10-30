package io.honeycomb.opentelemetry.android.shape

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.widget.TextView
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal const val UI_SCOPE = "io.honeycomb.ui"

/** Upstream log events that are turned into spans, and so must not also be exported as logs. */
internal val CONSUMED_EVENTS = setOf("app.widget.click")

/** Remembers the resumed Activity so a click event can be matched back to its live View. */
internal class ForegroundActivity : Application.ActivityLifecycleCallbacks {
    @Volatile
    var current: WeakReference<Activity>? = null

    override fun onActivityResumed(activity: Activity) {
        current = WeakReference(activity)
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {}

    override fun onActivityDestroyed(activity: Activity) {}
}

/**
 * Rebuilds this SDK's `click` span (scope `io.honeycomb.ui`) from upstream's `app.widget.click`
 * log event. The event only carries the view's numeric id and resource name, so `view.class`,
 * `view.text` and `view.accessibilityClassName` are recovered by looking the id up in the
 * foreground Activity. This runs synchronously on the emitting (UI) thread, so the view is live.
 */
class WidgetClickSpanLogRecordProcessor internal constructor(
    private val foreground: ForegroundActivity,
) : LogRecordProcessor {
    private val otel = AtomicReference<OpenTelemetry?>()

    fun setOpenTelemetry(openTelemetry: OpenTelemetry) = otel.set(openTelemetry)

    override fun onEmit(
        context: Context,
        logRecord: ReadWriteLogRecord,
    ) {
        val data = logRecord.toLogRecordData()
        if (data.eventName != "app.widget.click") return
        val tracer = otel.get()?.getTracer(UI_SCOPE) ?: return
        val id = logRecord.getAttribute(AttributeKey.stringKey("app.widget.id"))?.toIntOrNull()
        val activity = foreground.current?.get()
        val view: View? = if (id != null && id != View.NO_ID) activity?.findViewById(id) else null

        val timestamp = data.timestampEpochNanos.takeIf { it > 0 } ?: data.observedTimestampEpochNanos
        val span =
            tracer
                .spanBuilder("click")
                .setNoParent()
                .setStartTimestamp(timestamp, TimeUnit.NANOSECONDS)
                .startSpan()
        var entry: String? = null
        if (id != null && id != View.NO_ID) {
            span.setAttribute("view.id", id.toLong())
            activity?.resources?.let { res ->
                runCatching {
                    res.getResourcePackageName(
                        id,
                    )
                }.getOrNull()?.let { span.setAttribute("view.id.package", it) }
                entry = runCatching { res.getResourceEntryName(id) }.getOrNull()
                entry?.let { span.setAttribute("view.id.entry", it) }
            }
        }
        val text = (view as? TextView)?.text?.toString()
        view?.javaClass?.canonicalName?.let { span.setAttribute("view.class", it) }
        text?.let { span.setAttribute("view.text", it) }
        view?.accessibilityClassName?.toString()?.let { span.setAttribute("view.accessibilityClassName", it) }
        // Same rule as the old interaction module. The event's own app.widget.name is the numeric
        // id for views without a resource entry, which is not a useful name.
        (entry ?: text)?.let { span.setAttribute("view.name", it) }
        span.end(timestamp, TimeUnit.NANOSECONDS)
    }
}

/**
 * A processor cannot drop a record, and upstream's batch processor is registered before any
 * customizer, so records already turned into spans are dropped on the way out instead.
 */
class ConsumedEventsLogRecordExporter(
    private val delegate: LogRecordExporter,
) : LogRecordExporter {
    override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode =
        delegate.export(logs.filter { it.eventName !in CONSUMED_EVENTS })

    override fun flush(): CompletableResultCode = delegate.flush()

    override fun shutdown(): CompletableResultCode = delegate.shutdown()
}
