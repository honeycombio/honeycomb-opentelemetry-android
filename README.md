# Honeycomb OpenTelemetry Android

[![OSS Lifecycle](https://img.shields.io/osslifecycle/honeycombio/honeycomb-opentelemetry-android)](https://github.com/honeycombio/home/blob/main/honeycomb-oss-lifecycle-and-practices.md)
[![CircleCI](https://circleci.com/gh/honeycombio/honeycomb-opentelemetry-android.svg?style=shield)](https://circleci.com/gh/honeycombio/honeycomb-opentelemetry-android)

Honeycomb wrapper for [OpenTelemetry](https://opentelemetry.io) on Android.

**STATUS: this library is in BETA.** Data shapes are stable and safe for production. We are actively seeking feedback to ensure usability.

## Getting started

Add the following dependencies to your `build.gradle.kts`:
```
dependencies {
  implementation("io.honeycomb.android:honeycomb-opentelemetry-android:0.0.9")
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

## Configuration Options

| Option                  | Type                                            | Required? | Description                                                                                                                                                                              |
|-------------------------|-------------------------------------------------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `apiKey`                | String                                          | Yes       | Default [Honeycomb API Key](https://docs.honeycomb.io/working-with-your-data/settings/api-keys/) to use to send data directly to Honeycomb.                                              |
| `tracesApiKey`          | String                                          | No        | Dedicated API Key to use when sending traces.                                                                                                                                            |
| `metricsApiKey`         | String                                          | No        | Dedicated API Key to use when sending metrics.                                                                                                                                           |
| `logsApiKey`            | String                                          | No        | Dedicated API Key to use when sending logs.                                                                                                                                              |
| `dataset`               | String                                          | No        | Name of Honeycomb dataset to send traces to. Required if sending to a classic Honeycomb environment.                                                                                     |
| `metricsDataset`        | String                                          | No        | Name of Honeycomb dataset to send metrics to, instead of `dataset`.                                                                                                                      |
| `apiEndpoint`           | String                                          | No        | API endpoint to send data to.                                                                                                                                                            |
| `tracesEndpoint`        | String                                          | No        | API endpoint to send traces to. Overrides `apiEndpoint` for trace data.                                                                                                                  |
| `metricsEndpoint`       | String                                          | No        | API endpoint to send metrics to. Overrides `apiEndpoint` for metrics data.                                                                                                               |
| `logsEndpoint`          | String                                          | No        | API endpoint to send trace to. Overrides `apiEndpoint` for logs data.                                                                                                                    |
| `spanProcessor`         | io.opentelemetry.sdk.trace.SpanProcessor        | No        | Additional span processor to use.                                                                                                                                                        |
| `logRecordProcessor`     | io.opentelemetry.sdk.logs.LogRecordProcessor    | No       | Log Record processor to use.                                                                                                                                                             |
| `sampleRate`            | Int                                             | No        | Sample rate to apply (ie. a value of `40` means 1 in 40 traces will be exported).                                                                                                        |
| `debug`                 | Boolean                                         | No        | Enable debug logging.                                                                                                                                                                    |
| `serviceName`           | String?                                         | No        | This determines the Honeycomb service to send data to, and also appears as the contents of the `service.name` resource attribute.                                                        |
| `serviceVersion`        | String?                                         | No        | Current version of your application. Appears as the value of the `service.version` resource attribute.                                                                                   |
| `resourceAttributes`    | Map<String, String>                             | No        | Attributes to attach to outgoing resources.                                                                                                                                              |
| `headers`               | Map<String, String>                             | No        | Headers to include on exported data.                                                                                                                                                     |
| `tracesHeaders`         | Map<String, String>                             | No        | Headers to add to exported trace data.                                                                                                                                                   |
| `metricsHeaders`        | Map<String, String>                             | No        | Headers to add to exported metrics data.                                                                                                                                                 |
| `logsHeaders`           | Map<String, String>                             | No        | Headers to add to exported logs data.                                                                                                                                                    |
| `timeout`               | Duration                                        | No        | Timeout used by exporter when sending data.                                                                                                                                              |
| `tracesTimeout`         | Duration                                        | No        | Timeout used by traces exporter. Overrides `timeout` for trace data.                                                                                                                     |
| `metricsTimeout`        | Duration                                        | No        | Timeout used by metrics exporter. Overrides `timeout` for metrics data.                                                                                                                  |
| `logsTimeout`           | Duration                                        | No        | Timeout used by logs exporter. Overrides `timeout` for logs data.                                                                                                                        |
| `protocol`              | io.honeycomb.opentelemetry.android.OtlpProtocol | No        | Protocol to use when sending data.                                                                                                                                                       |
| `tracesProtocol`        | io.honeycomb.opentelemetry.android.OtlpProtocol | No        | Overrides `protocol` for trace data.                                                                                                                                                     |
| `metricsProtocol`       | io.honeycomb.opentelemetry.android.OtlpProtocol | No        | Overrides `protocol` for metrics data.                                                                                                                                                   |
| `logsProtocol`          | io.honeycomb.opentelemetry.android.OtlpProtocol | No        | Overrides `protocol` for logs data.                                                                                                                                                      |
| `offlineCachingEnabled` | Boolean                                         | No        | Whether to enable offline caching for telemetry (default: false). Warning: this feature is still in alpha and may be unstable. For more details, see [Offline Caching](#offline-caching) |

## Standard Attributes
All telemetry emitted will have the following resource attributes attached:

- `device.manufacturer`: Manufacturer of the device, as reported by [`android.os.Build.MANUFACTURER`](https://developer.android.com/reference/android/os/Build#MANUFACTURER)
- `device.model.identifier`: Model of the device, as reported by [`android.os.Build.MODEL`](https://developer.android.com/reference/android/os/Build#MODEL)
- `device.model.name`: see `device.model.identifier`
- `honeycomb.distro.runtime_version`: Version of Android on the device. See also `os.version`.
- `honeycomb.distro.version`: Version of the Honeycomb SDK being used.
- `os.description`: String containing Android version, build ID, and SDK level.
- `os.name`: "android"
- `os.type`: "linux"
- `os.version`: The value of [`android.os.Build.VERSION.RELEASE`](https://developer.android.com/reference/android/os/Build.VERSION#RELEASE)
- `rum.sdk.version`: Version of the OpenTelemetry Android SDK being used.
- `screen.name`: Name of the current Activity or Fragment (see [Navigation](#navigation-instrumentation) section below)
- `service.name`: The name of your application, as provided via `setServiceName()`, or `unknown_service` if unset.
- `service.version`: Optional. The version of your application, as provided via `setServiceVersion()
- `telemetry.sdk.language`: "android"
- `telemetry.sdk.name`: "opentelemetry"
- `telemetry.sdk.version`: version of the base OpenTelemetry SDK being used

## Auto-instrumentation

To enable all OpenTelemetry auto-instrumentation, simply include `android-agent` as a dependency:
* [`io.opentelemetry.android:android-agent`](https://github.com/open-telemetry/opentelemetry-android/tree/main)

If you want to pick and choose which auto-instrumentation to include, you can instead add dependencies for whichever components you would like:
* [`io.opentelemetry.android:instrumentation-activity`](https://github.com/open-telemetry/opentelemetry-android/tree/main/instrumentation/activity)
* [`io.opentelemetry.android:instrumentation-anr`](https://github.com/open-telemetry/opentelemetry-android/tree/main/instrumentation/anr)
* [`io.opentelemetry.android:instrumentation-crash`](https://github.com/open-telemetry/opentelemetry-android/tree/main/instrumentation/crash)
* [`io.opentelemetry.android:instrumentation-fragment`](https://github.com/open-telemetry/opentelemetry-android/tree/main/instrumentation/fragment)
* [`io.opentelemetry.android:instrumentation-slowrendering`](https://github.com/open-telemetry/opentelemetry-android/tree/main/instrumentation/slowrendering)

The following additional auto-instrumentation is implemented in this library:
* `honeycomb-opentelemetry-android-interaction` &mdash; UI interaction in XML-based Activities.

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

### UI Interaction Instrumentation

Various touch events are instrumented for `TextView` fields, such as:
* `Touch Began` - A touch started
* `Touch Ended` - A touch ended
* `click` - A "tap"

These events may have the following attributes.
* `view.class` - e.g. `"android.widget.Button"`
* `view.accessibilityClassName` - The [`accessibilityClassName`](https://developer.android.com/reference/android/widget/TextView#getAccessibilityClassName()) for the view.
   Usually the same as `view.class`.
* `view.text` - The text of the `TextView`.
* `view.id` - The XML ID of the view element, as an integer.
* `view.id.entry` - The text form of the XML ID of the view element.
* `view.id.package` - The package for the XML ID of the view.
* `view.name` - The "best" available name of the view, given the other identifiers. Usually the same as `view.id.entry`.

### Navigation Instrumentation

If you have included the activity or fragment auto-instrumentation, that library will track navigation between Activities and Fragments in your Android application. It automatically sets the `screen.name` attribute on spans to the name of the current Activity or Fragment that a user is viewing. This provides visibility into user navigation patterns and helps correlate other telemetry with the specific screen where events occurred.

## Manual Instrumentation

### Context Propagation

In most cases, OpenTelemetry context will be propagated correctly across function calls, etc. By default, OpenTelemetry context is stored in a `java.lang.ThreadLocal`. However, Kotlin Coroutines may operate across multiple threads, and therefore do not automatically inherit the correct OpenTelemetry context.

Instead, context must be propagated manually, via the [OpenTelemetry Kotlin Extensions](https://github.com/open-telemetry/opentelemetry-java/tree/main/extensions/kotlin).

```gradle
dependencies {
  implementation("io.opentelemetry:opentelemetry-extension-kotlin:1.47.0")
}
```

Once these are installed, replace any `launch` calls with

```kotlin
launch(Span.current().asContextElement()) {

}
```

### Manual Error Logging

Exceptions may be recorded as Log records using the `logException` method. This can be used for logging
any caught exceptions in your own code that will not be logged by our crash instrumentation.

Below is an example of logging an Exception object using several custom attributes.

```kotlin
try {
    // ...
} catch (e: Exception) {
    Honeycomb.logException(
        otel,
        e,
        Attributes.of(
            AttributeKey.stringKey("user.name"), "bufo",
            AttributeKey.longKey("user.id"), 1
        ),
        Thread.currentThread())
}
```

| Argument   | Type             | Is Required | Description                                                                       |
|------------|------------------|-------------|-----------------------------------------------------------------------------------|
| otel       | OpenTelemetryRum | true        | The OpenTelemetryRum instance to use for logging.                                 |
| exception  | Throwable        | true        | The exception itself. Attributes will be automatically added to the log record.   |
| attributes | Attributes?      | false       | Additional attributes you would like to log along with the default ones provided. |
| thread     | Thread?          | false       | Thread where the error occurred. Add this to include the thread as attributes.    |

### Android Compose
#### Setup
Android Compose instrumentation is included in a standalone library. Add the following to your dependencies in `build.gradle.kts`:
```
dependencies {
  implementation("io.honeycomb.android:honeycomb-opentelemetry-android-compose:0.0.9")
}
```

After you initialize the `Honeycomb` sdk, wrap your entire app in a `CompositionLocalProvider` that provides `LocalOpenTelemetryRum`, as so:

```kotlin
import io.honeycomb.opentelemetry.android.compose.LocalOpenTelemetryRum

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ExampleApp
        val otelRum = app.otelRum

        setContent {
            CompositionLocalProvider(LocalOpenTelemetryRum provides otelRum) {
                // app content
            }
        }
    }
}
```

#### Usage
Wrap your Composables with `HoneycombInstrumentedComposable(name: String)`, like so:

```kotlin
@Composable
private fun MyComposable() {
    HoneycombInstrumentedComposable("main view") {
        // ...
    }
}
```

This will measure and emit instrumentation for your Composable's render times, ex:

Specifically, it will emit 2 kinds of span for each composable that is wrapped:

`View Render` spans encompass the entire rendering process, from initialization to appearing on screen. They include the following attributes:
- `view.name` (string): the name passed to `HoneycombInstrumentedComposable`
- `view.renderDuration` (double): amount of time in seconds to spent initializing the contents of `HoneycombInstrumentedComposable`
- `view.totalDuration` (double): amount of time in seconds from when the contents of `HoneycombInstrumentedComposable` start initializing to when the contents appear on screen

`View Body` spans encompass just the contents of the `HoneycombInstrumentedView`, and include the following attributes:
- `view.name` (string): the name passed to `HoneycombInstrumentedComposable`

### Adding a Custom Span Processor

You can implement and register your own custom span processor with the Honeycomb SDK. This allows you to perform custom operations on spans before they are exported, such as adding application-specific attributes or filtering certain spans at the application level.

```kotlin
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

// Create a custom span processor
class MyCustomSpanProcessor : SpanProcessor {
    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        // Add custom logic when a span starts
        // For example, add a custom attribute to every span:
        span.setAttribute("custom.attribute", "custom_value")
    }


    override fun onEnd(span: ReadableSpan) {
        // Add custom logic when a span ends
    }

    override fun shutdown(): CompletableResultCode {
        // Clean up any resources
        return CompletableResultCode.ofSuccess()
    }

    override fun isStartRequired(): Boolean = true

    override fun isEndRequired(): Boolean = true
}

// Then when configuring the SDK, add your processor:
val options = HoneycombOptions.builder(this)
    .setApiKey("YOUR-API-KEY")
    .setServiceName("YOUR-SERVICE-NAME")
    .setSpanProcessor(MyCustomSpanProcessor())
    .build()

otelRum = Honeycomb.configure(this, options)
```

To use multiple custom span processors, you can combine them using OpenTelemetry's `SpanProcessor.composite()` helper:

```kotlin
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

// Combine multiple span processors
val combinedProcessor = SpanProcessor.composite(
    FirstSpanProcessor(),
    SecondSpanProcessor(),
)

// Add the combined processor to your options
val options = HoneycombOptions.builder(this)
    .setApiKey("YOUR-API-KEY")
    .setServiceName("YOUR-SERVICE-NAME")
    .setSpanProcessor(combinedProcessor)
    .build()

otelRum = Honeycomb.configure(this, options)
```

## Offline Caching

Set the `offlineCachingEnabled` option to enable disk buffering for outgoing telemetry. This will cache your telemetry in the event of network failures and continue to retry exporting your telemetry for up to 18 hours. You will also see a minimum delay in exporting telemetry, of at least 30 seconds.

This feature is currently in alpha and may be unstable. It is currently off by default.
