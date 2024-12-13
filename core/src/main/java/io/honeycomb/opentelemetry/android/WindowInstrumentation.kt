package io.honeycomb.opentelemetry.android

import android.app.Activity
import android.app.Application
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.ActionMode
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.core.view.children
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.api.trace.Span
import kotlin.math.roundToInt

private const val INSTRUMENTATION_NAME = "@honeycombio/instrumentation-ui"

enum class TouchEventType(val spanName: String) {
    TOUCH_BEGAN("Touch Began"),
    TOUCH_ENDED("Touch Ended"),
    CLICK("click"),
}

private class ViewAttributes(activity: Activity, view: View) {
    val className: String? = view.javaClass.canonicalName

    val text: String? = if (view is TextView) view.text.toString() else null

    val accessibilityClassName: String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.accessibilityClassName.toString()
        } else {
            null
        }

    val id: Int? = if (view.id != View.NO_ID) view.id else null

    val idPackage: String? = id?.let { activity.resources.getResourcePackageName(it) }
    val idEntry: String? = id?.let { activity.resources.getResourceEntryName(it) }

    val name: String? get() = idEntry ?: text

    fun setAttributes(span: Span) {
        className?.let { span.setAttribute("view.class", it) }
        text?.let { span.setAttribute("view.text", it) }
        accessibilityClassName?.let { span.setAttribute("view.accessibilityClassName", it) }
        id?.let { span.setAttribute("view.id", it.toLong()) }
        idPackage?.let { span.setAttribute("view.id.package", it) }
        idEntry?.let { span.setAttribute("view.id.entry", it) }
        name?.let { span.setAttribute("view.name", it) }
    }
}

private fun recordTouchEvent(
    otelRum: OpenTelemetryRum,
    type: TouchEventType,
    activity: Activity,
    x: Int,
    y: Int,
) {
    val contentView = activity.findViewById<View>(android.R.id.content)
    val textView = findTextViewAtPosition(contentView, x, y)
    if (textView != null) {
        val otel = otelRum.openTelemetry
        val tracer = otel.getTracer(INSTRUMENTATION_NAME)
        val span = tracer.spanBuilder(type.spanName).startSpan()
        ViewAttributes(activity, textView).setAttributes(span)
        span.end()
    }
}

private fun findTextViewAtPosition(
    content: View,
    x: Int,
    y: Int,
): TextView? {
    if (content is ViewGroup) {
        if (content.isShown) {
            // Empirically, this seems to be the order that Android uses to find the touch target,
            // even if the developer has used setZ to override the render order.
            for (child in content.children.toList().reversed()) {
                if (child.isShown) {
                    val view = findTextViewAtPosition(child, x, y)
                    if (view != null) {
                        return view
                    }
                }
            }
        }
    }
    if (content is TextView) {
        val hitRect = Rect()
        content.getHitRect(hitRect)

        val location = IntArray(2)
        content.getLocationInWindow(location)

        val left = location[0]
        val top = location[1]
        val right = left + hitRect.width()
        val bottom = top + hitRect.height()

        val rect = Rect(left, top, right, bottom)

        if (rect.contains(x, y)) {
            return content
        }
    }
    return null
}

private class InteractionGestureListener(
    val otelRum: OpenTelemetryRum,
    val activity: Activity,
) : SimpleOnGestureListener() {
    override fun onSingleTapUp(event: MotionEvent): Boolean {
        val x = event.x.roundToInt()
        val y = event.y.roundToInt()
        recordTouchEvent(otelRum, TouchEventType.CLICK, activity, x, y)
        return super.onSingleTapUp(event)
    }
}

private class InteractionWindowCallback(
    val otelRum: OpenTelemetryRum,
    val activity: Activity,
    val wrapped: Window.Callback,
) : Window.Callback {
    val gestureDetector = GestureDetector(activity, InteractionGestureListener(otelRum, activity))

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return wrapped.dispatchKeyEvent(event)
    }

    override fun dispatchKeyShortcutEvent(event: KeyEvent?): Boolean {
        return wrapped.dispatchKeyShortcutEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            val type =
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> TouchEventType.TOUCH_BEGAN
                    MotionEvent.ACTION_UP -> TouchEventType.TOUCH_ENDED
                    MotionEvent.ACTION_POINTER_DOWN -> TouchEventType.TOUCH_BEGAN
                    MotionEvent.ACTION_POINTER_UP -> TouchEventType.TOUCH_ENDED
                    else -> null
                }
            if (type != null) {
                val x = event.x.roundToInt()
                val y = event.y.roundToInt()
                recordTouchEvent(otelRum, type, activity, x, y)
            }
            gestureDetector.onTouchEvent(event)
        }
        return wrapped.dispatchTouchEvent(event)
    }

    override fun dispatchTrackballEvent(event: MotionEvent?): Boolean {
        return wrapped.dispatchTrackballEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
        return wrapped.dispatchGenericMotionEvent(event)
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean {
        return wrapped.dispatchPopulateAccessibilityEvent(event)
    }

    override fun onCreatePanelView(featureId: Int): View? {
        return wrapped.onCreatePanelView(featureId)
    }

    override fun onCreatePanelMenu(
        featureId: Int,
        menu: Menu,
    ): Boolean {
        return wrapped.onCreatePanelMenu(featureId, menu)
    }

    override fun onPreparePanel(
        featureId: Int,
        view: View?,
        menu: Menu,
    ): Boolean {
        return wrapped.onPreparePanel(featureId, view, menu)
    }

    override fun onMenuOpened(
        featureId: Int,
        menu: Menu,
    ): Boolean {
        return wrapped.onMenuOpened(featureId, menu)
    }

    override fun onMenuItemSelected(
        featureId: Int,
        item: MenuItem,
    ): Boolean {
        return wrapped.onMenuItemSelected(featureId, item)
    }

    override fun onWindowAttributesChanged(params: WindowManager.LayoutParams?) {
        return wrapped.onWindowAttributesChanged(params)
    }

    override fun onContentChanged() {
        return wrapped.onContentChanged()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        return wrapped.onWindowFocusChanged(hasFocus)
    }

    override fun onAttachedToWindow() {
        return wrapped.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        return wrapped.onDetachedFromWindow()
    }

    override fun onPanelClosed(
        featureId: Int,
        menu: Menu,
    ) {
        return wrapped.onPanelClosed(featureId, menu)
    }

    override fun onSearchRequested(): Boolean {
        return wrapped.onSearchRequested()
    }

    override fun onSearchRequested(event: SearchEvent?): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            wrapped.onSearchRequested(event)
        } else {
            // Do nothing
            false
        }
    }

    override fun onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? {
        return wrapped.onWindowStartingActionMode(callback)
    }

    override fun onWindowStartingActionMode(
        callback: ActionMode.Callback?,
        type: Int,
    ): ActionMode? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            wrapped.onWindowStartingActionMode(callback, type)
        } else {
            null
        }
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        return wrapped.onActionModeStarted(mode)
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        return wrapped.onActionModeFinished(mode)
    }
}

private class InteractionLifecycleCallbacks(
    val otelRum: OpenTelemetryRum,
) : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(
        activity: Activity,
        bundle: Bundle?,
    ) {
        activity.window.callback = InteractionWindowCallback(otelRum, activity, activity.window.callback)
    }

    override fun onActivityStarted(p0: Activity) {}

    override fun onActivityResumed(p0: Activity) {}

    override fun onActivityPaused(p0: Activity) {}

    override fun onActivityStopped(p0: Activity) {}

    override fun onActivitySaveInstanceState(
        p0: Activity,
        p1: Bundle,
    ) {}

    override fun onActivityDestroyed(p0: Activity) {}
}

@AutoService(AndroidInstrumentation::class)
class WindowInstrumentation : AndroidInstrumentation {
    override fun install(
        application: Application,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        application.registerActivityLifecycleCallbacks(InteractionLifecycleCallbacks(openTelemetryRum))
    }
}
