# Honeycomb Android SDK changelog

## Unreleased

* feat: Add a `setLogRecordProcessor()` function to `HoneycombOptions` builder to allow clients to supply custom log record processors.

## v0.0.9
* maint: bump androidx.core:core-ktx from 1.15.0 to 1.16.0
* fix: Fix a bug in unhandled exception instrumentation that caused hangs instead of crashes

## v0.0.8
* refactor: Switch to using android agent as dependency
* feat: Allow optional API key for custom endpoint

## v0.0.7

* feat: install.id added to all spans
* feat: Add OS and Device resource attributes

## v0.0.6

* feat: Add logException function for manual error reporting
* refactor: Change dependencies to require users to explicitly include auto-instrumentation
* cleanup: Update instrumentation names to use reverse url notation (`io.honeycomb.*`) instead of `@honeycombio/instrumentation-*` notation
* feat: Enable telemetry caching for offline support
* feat: Add `setServiceVersion()` function to `HoneycombOptions` to allow clients to supply current application version

## v0.0.5-alpha

* feat: Add a `setSpanProcessor()` function to `HoneycombOptions` builder to allow clients to supply custom span processors

## v0.0.4-alpha

* feat: Add deterministic sampler (configurable through the `sampleRate` option)
* refactor: Move touch auto-instrumentation to separate package
* feat: Add instrumentation helpers for Android Compose
* add BaggageSpanProcessor
* add debug logging in configure
* include `telemetry.sdk.language` resource attribute

## v0.0.3-alpha

* fix: Fixes a bug with duplicate auto-instrumentation

## v0.0.2-alpha

* feat: Add touch auto-instrumentation for XML-based TextViews
* feat: Use a BatchSpanProcessor
* feat: Implement a debug option for more verbose logging
* maint: Various improvements to smoke tests
* maint: Add spotless linter

## v0.0.1-alpha

* Initial release
* Basic configuration helpers
