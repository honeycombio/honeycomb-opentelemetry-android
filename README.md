# Honeycomb OpenTelemetry Android

[![OSS Lifecycle](https://img.shields.io/osslifecycle/honeycombio/honeycomb-opentelemetry-android)](https://github.com/honeycombio/home/blob/main/honeycomb-oss-lifecycle-and-practices.md)
[![CircleCI](https://circleci.com/gh/honeycombio/honeycomb-opentelemetry-android.svg?style=shield)](https://circleci.com/gh/honeycombio/honeycomb-opentelemetry-android)

Honeycomb wrapper for [OpenTelemetry](https://opentelemetry.io) on Android.

**STATUS: this library is EXPERIMENTAL.** Data shapes are unstable and not safe for production. We are actively seeking feedback to ensure usability.

## Getting started

Add the following dependencies to your `build.gradle.kts`:
```
dependencies {
  implementation("io.honeycomb.android:honeycomb-opentelemetry-android:0.0.1-alpha")
}
```

To configure the SDK in your Application class:
```
import io.honeycomb.opentelemetry.android.Honeycomb
import io.honeycomb.opentelemetry.android.HoneycombOptions
import io.opentelemetry.android.OpenTelemetryRum

class ExampleApp: Application() {
    var otelRum: OpenTelemetryRum? = null

    override fun onCreate() {
        super.onCreate()

        val options = HoneycombOptions.builder(this)
            .setApiKey("YOUR-API-KEY")
            .setServiceName("YOUR-SERVICE-NAME")
            .build()

        otelRum = Honeycomb.configure(this, options)
    }
}
```

To manually send a span:
```
    val app = application as ExampleApp
    val otelRum = app.otelRum
    val otel = otelRum?.openTelemetryA

    val tracer = otel?.getTracer("YOUR-INSTRUMENTATION-NAME")
    val span = tracer?.spanBuilder("YOUR-SPAN-NAME")?.startSpan()

    span?.end()
```

## Auto-instrumentation

The following auto-instrumentation libraries are automatically included:
* [`io.opentelemetry.android:android-agent`](https://github.com/open-telemetry/opentelemetry-android/tree/main)
* [`io.opentelemetry.android:instrumentation-activity`](https://github.com/open-telemetry/opentelemetry-android/tree/main/instrumentation/activity)
* [`io.opentelemetry.android:instrumentation-anr`](https://github.com/open-telemetry/opentelemetry-android/tree/main/instrumentation/anr)
* [`io.opentelemetry.android:instrumentation-crash`](https://github.com/open-telemetry/opentelemetry-android/tree/main/instrumentation/crash)
* [`io.opentelemetry.android:instrumentation-slowrendering`](https://github.com/open-telemetry/opentelemetry-android/tree/main/instrumentation/slowrendering)

### Activity Lifecycle Instrumentation

A trace is emitted before, during, and after each stage of the [Android Activity Lifecycle](https://developer.android.com/guide/components/activities/activity-lifecycle).

#### Trace Types
* AppStart
  * name: `AppStart`
  * attributes:
    * `start.type`: `"hot"` or `"cold"`
* Created
  * name: `Created`
  * events:
    * `activityPreCreated`, `activityCreated`, `activityPostCreated`
    * `activityPreStarted`, `activityStarted`, `activityPostStarted`
    * `activityPreResumed`, `activityResumed`, `activityPostResumed`
* Paused
  * name: `Paused`
  * events:
    * `activityPrePaused`, `activityPaused`, `activityPostPaused`
* Stopped
  * name: `Stopped`
  * events:
    * `activityPreStopped`, `activityStopped`, `activityPostStopped`
* Destroyed
  * name: `Destroyed`
  * events:
    * `activityPreDestroyed`, `activityDestroyed`, `activityPostDestroyed`

#### Common Attributes:
* `activityName` - The unqualified name for the activity, such as `"MainActivity"`

### ANR (Application Not Responding) Instrumentation

A trace is emitted whenever the app becomes unresponsive for longer than approximately 5 seconds ([ANR](https://developer.android.com/topic/performance/vitals/anr)).

* name: `ANR`
* attributes:
  * `exception.stacktrace`

### Crash Instrumentation

A trace is emitted whenever an uncaught exception is going to terminate the program. This is in addition to whatever other crash reporting is enabled. These traces are flushed as the app is shutting down, so the app does not need to be re-opened for them to be sent.

* name: `UncaughtException`
* attributes:
  * `exception.name`
  * `exception.message`
  * `exception.stacktrace`
  * `exception.escaped`
  * `thread.name`
  * `thread.id`

### Slow Rendering Instrumentation

A trace is emitted whenever a frame is rendered too slowly, according to Android's [Slow rendering guidelines](https://developer.android.com/topic/performance/vitals/render).

* name: `slowRenders` (>16ms) or `frozenRenders` (>700ms)
* attributes:
  * `activity.name` - the fully-qualified name of the activity, such as `"io.honeycomb.opentelemetry.android.example/.MainActivity"`

### Network Instrumentation
Network instrumentation is not included by default but can be configured on top of this distro. We test with the following OpenTelemetry instrumentation packages:

* [`io.opentelemetry.android:okhttp-3.0`](https://github.com/open-telemetry/opentelemetry-android/tree/main/instrumentation/okhttp/okhttp-3.0)

Configuration of each of these packages is described in their respective READMEs.