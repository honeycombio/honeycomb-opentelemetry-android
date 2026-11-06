# Migration Guide

## Migrating to v1.1.0

Version 1.1.0 updates the underlying OpenTelemetry Android SDK from 1.0.1 to 1.8.1. Your telemetry does not change by default, but the minimum build requirements do.

### New minimum requirements

- `minSdk` 23 or newer (1.0.0 supported 21)
- Android Gradle Plugin 9.1 or newer
- `compileSdk` 37 or newer
- Kotlin 2.3 or newer, for apps that use Kotlin

For comparison, 1.0.0 was built with Android Gradle Plugin 8.13, `compileSdk` 36 and Kotlin 2.1.

AGP 9.1 and `compileSdk` 37 come from `androidx.core` 1.19.1, which OpenTelemetry Android 1.8.1 depends on. Kotlin 2.3 is needed to read the Kotlin 2.4 metadata of `kotlin-stdlib` 2.4.21.

If you cannot upgrade yet, stay on 1.0.0. If your app is on Android 8 - 11 (API 26 - 30) without core library desugaring, 1.0.0 crashes when an activity pauses (`NoSuchMethodError` for `Duration.toSeconds()`). The workaround is to enable core library desugaring, or to disable the slow rendering instrumentation with `setDisabledInstrumentation(listOf("slowrendering"))`.

### Code changes

- `Honeycomb.configure(...).openTelemetry` is no longer an `OpenTelemetrySdk`. Remove any cast to `OpenTelemetrySdk` and use the `OpenTelemetry` API.
- If you wrote your own `AndroidInstrumentation`, its `install` method now takes `(context: Context, openTelemetryRum: OpenTelemetryRum)`.

### Telemetry names

OpenTelemetry Android renamed some attributes and events. This SDK keeps the names it has always emitted, so dashboards, triggers and queries keep working:

| Previous name (default) | Latest OpenTelemetry name |
|---|---|
| `screen.name` | `app.screen.name` |
| `last.screen.name` | `app.screen.previous.name` |
| `device.crash` (event) | `app.crash` |
| `network.status` | `network.connection.type` |

To switch to the latest names, call `setUseLatestSemanticConventions(true)` on the options builder. Only one set of names is emitted, and the setting applies to the whole process.

The resource also includes three new attributes: `android.os.api_level`, `app.installation.id` and `os.build_id`.

## Migrating to v1.0.0

Version 1.0.0 of the Honeycomb Android SDK updates the underlying OpenTelemetry Android SDK from 0.11.0-alpha to 1.0.1. This update includes breaking changes to telemetry shape for automatically instrumented events.

### Breaking Changes to Telemetry Shape

The OpenTelemetry Android SDK made significant changes to how certain types of events are reported. Events that were previously reported as **spans** are now reported as **log events**. This affects ANR detection and slow rendering detection.

#### ANR (Application Not Responding) Detection

**Changed in OpenTelemetry Android v0.14.0**

ANRs are now reported as log events instead of spans.

**Before (< v1.0.0):**
- Signal type: Span
- Scope name: `io.opentelemetry.anr`
- Span name: `ANR`
- Key attributes:
  - `exception.stacktrace`

**After (>= v1.0.0):**
- Signal type: Log event
- Scope name: `io.opentelemetry.anr`
- Event name: `device.anr`
- Key attributes:
  - `exception.stacktrace`
  - `thread.name` (always "main")
  - `thread.id`

#### Slow Rendering / Jank Detection

**Changed in OpenTelemetry Android v0.15.0**

Slow rendering events (jank) are now reported as log events instead of spans, with a new scope name.

**Before (< v1.0.0):**
- Signal type: Span
- Scope name: `io.opentelemetry.slow-rendering`
- Span names: `slowRenders`, `frozenRenders`

**After (>= v1.0.0):**
- Signal type: Log event
- Scope name: `app.jank`
- Event name: `app.jank`
- Key attributes:
  - `app.jank.threshold` (in seconds: `0.016` for slow, `0.7` for frozen)
  - `app.jank.frame_count` (number of janky frames)
  - `screen.name` (which screen the jank occurred on)
  - `app.jank.period` (sampling period in seconds)

### Additional Resources

For more information about the OpenTelemetry Android SDK changes:
- [OpenTelemetry Android Releases](https://github.com/open-telemetry/opentelemetry-android/releases)
