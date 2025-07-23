# Honeycomb ProGuard UUID Plugin

A simple Gradle plugin that automatically injects unique UUIDs into Android manifests for ProGuard mapping correlation.

## What it does

- Generates a unique UUID for each build
- Injects the UUID into your Android manifest as metadata
- Creates a properties file containing the generated UUID

## Setup

### 1. Apply the plugin

In your app's `build.gradle.kts` plugins block:

```kotlin
plugins {
    id("io.honeycomb.proguard-uuid")
}
```

### 2. Add metadata tag to your AndroidManifest.xml

In your `src/main/AndroidManifest.xml`, add this inside the `<application>` tag:

```xml
<application>
    <!-- Your existing app content -->

    <meta-data
        android:name="io.honeycomb.proguard.uuid"
        android:value="${PROGUARD_UUID}" />
</application>
```

The placeholder value HAS to be set to `PROGUARD_UUID`.

### 3. Build and run your app

The plugin automatically runs and injects a UUID to the **merged** android manifest file:

```xml
<meta-data
    android:name="io.honeycomb.proguard.uuid"
    android:value="6A8CB813-45F6-3652-AD33-778FD1EAB196" />
```

You may see that your `src/main/AndroidManifest.xml` file still has the `PROGUARD_UUID` placeholder.
This is expected. The generated manifest file after the app builds will be the one to include the
meta-data tag with the actual uuid value.

## Accessing the generated UUID

Within the app, you can access the metadata:
```kotlin
val app = application as ExampleApp
val applicationInfo = app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
val uuid = applicationInfo.metaData?.getString("io.honeycomb.proguard.uuid")
```

The plugin also generates a properties file containing the generated UUID for the build. You can find the
UUID under your `build` directory in the `generated/honeycomb/proguard-uuid.properties` file. The file will
contain a key and a value:

```
io.honeycomb.proguard.uuid=6A8CB813-45F6-3652-AD33-778FD1EAB196
```

## Use cases for the plugin

This plugin is intended to be used alongside the Honeycomb Android SDK. The UUID generated from this plugin
will be collected by the Android SDK and emitted as a value to the `app.debug.proguard_uuid` attribute.
Proguard files are also expected to be versioned with the generated UUID. For example,
`6A8CB813-45F6-3652-AD33-778FD1EAB196.txt`.
