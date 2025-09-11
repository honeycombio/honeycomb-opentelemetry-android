# Honeycomb Android SDK changelog

## Unreleased

## v0.0.18

* feat: expose configured resource as public property on Honeycomb class
* feat: emit source files as a structured stacktrace attribute (#166)
* chore(deps): bump org.mockito:mockito-core from 5.18.0 to 5.19.0
* chore(deps): bump androidx.compose.runtime:runtime-android from 1.8.3 to 1.9.0 
* chore(deps): bump androidx.lifecycle:lifecycle-runtime-ktx from 2.9.2 to 2.9.3
* chore(deps): bump com.google.android.material:material from 1.12.0 to 1.13.0
* chore(deps): bump com.android.tools.build:gradle from 8.12.1 to 8.13.0
* chore(deps): bump agp from 8.12.1 to 8.13.0
* chore(deps): bump androidx.compose:compose-bom from 2025.08.00 to 2025.08.01
* chore(deps): bump io.honeycomb.proguard-uuid from 0.0.16 to 0.0.17
* chore(deps): bump net.bytebuddy.byte-buddy-gradle-plugin from 1.17.6 to 1.17.7

## v0.0.17

* feat: add proguard uuid attr to logs (#157)
* chore(deps): bump androidx.test.espresso:espresso-core from 3.6.1 to 3.7.0 (#151)
* chore(deps): bump androidx.test.ext:junit from 1.2.1 to 1.3.0 (#150)
* chore(deps): bump com.android.tools.build:gradle from 8.11.1 to 8.12.0 (#149)
* chore(deps): bump org.mockito.kotlin:mockito-kotlin from 5.4.0 to 6.0.0 (#148)
* chore(deps): bump agp from 8.11.1 to 8.12.0 (#147)
* chore(deps): bump androidx.lifecycle:lifecycle-runtime-ktx from 2.9.1 to 2.9.2 (#146)
* chore(deps): bump androidx.compose:compose-bom from 2025.06.01 to 2025.07.00 (#145)

## v0.0.16

* fix: revert kotlin version to maintain compatibility with React Native (#154)

## v0.0.15

* feat: Make `HoneycombOptions` Builder extendable (#152)

## v0.0.14

* fix: fix plugin config when publishing to maven central (#142)

## v0.0.13

* chore: set up distribution for gradle plugin (#140)
* fix: desugaring is no longer required for SDK > 26. (#139)
* chore(deps): bump agp from 8.11.0 to 8.11.1 (#134)
* chore(deps): bump com.diffplug.spotless from 7.0.4 to 7.1.0 (#135)
* chore(deps): bump com.squareup.okhttp3:okhttp from 4.12.0 to 5.1.0 (#133)
* docs: improve consistency between Android and iOS (#138)
* feat: emit the proguard uuid as a resource attribute (#137)
* feat: setup a gradle plugin that generates and injects UUID (#136)
* feat: Create structured stack trace fields (#132)
* chore: Update sonatype configuration because of OSSRH sunset 🌅. (#131)

## v0.0.12

* chore(deps): bump kotlin from 2.1.21 to 2.2.0 (#129)
* chore(deps): bump agp from 8.10.1 to 8.11.0 (#128)
* chore(deps): bump net.bytebuddy.byte-buddy-gradle-plugin from 1.17.5 to 1.17.6 (#127)
* chore(deps): bump androidx.compose.runtime:runtime-android from 1.8.2 to 1.8.3 (#126)
* chore(deps): bump androidx.appcompat:appcompat from 1.7.0 to 1.7.1 (#122)
* chore(deps): bump androidx.test.uiautomator:uiautomator from 2.4.0-alpha03 to 2.4.0-alpha04 (#121)
* chore(deps): bump androidx.compose:compose-bom from 2025.05.01 to 2025.06.00 (#120)
* chore(deps): bump com.diffplug.spotless from 7.0.3 to 7.0.4 (#119)
* chore(deps): bump agp from 8.10.0 to 8.10.1 (#118)
* chore(deps): bump androidx.lifecycle:lifecycle-runtime-ktx from 2.9.0 to 2.9.1 (#123)
* docs: add section on trace propagation to readme (#124) | Mustafa Haddara
* chore(deps): bump androidx.test.uiautomator:uiautomator from 2.4.0-alpha02 to 2.4.0-alpha03 (#117)
* chore(deps): bump androidx.compose.runtime:runtime-android from 1.8.1 to 1.8.2 (#116)
* chore(deps): bump org.mockito:mockito-core from 5.17.0 to 5.18.0 (#115)
* chore(deps): bump androidx.compose:compose-bom from 2025.05.00 to 2025.05.01 (#114)
* chore(deps): bump kotlin from 2.1.20 to 2.1.21 (#113)

## v0.0.11

* fix: Device ID is now the android id and not the string `android_id`

## v0.0.10

* maint: upgrade OpenTelemetry Android to 0.11.0-alpha
* feat: add `BaggageLogRecordProcessor`
* feat: Add a `setLogRecordProcessor()` function to `HoneycombOptions` builder to allow clients to supply custom log record processors.
* feat: telemetry distro attrs

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
