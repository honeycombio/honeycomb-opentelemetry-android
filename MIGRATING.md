# Migration Guide

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
