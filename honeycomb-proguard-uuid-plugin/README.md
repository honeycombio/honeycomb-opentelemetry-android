# Honeycomb ProGuard UUID Plugin

A simple Gradle plugin that automatically injects unique UUIDs into Android manifests for ProGuard mapping correlation.

## What it does

- Generates a unique UUID for each build
- Injects the UUID into your Android manifest as metadata

## Setup

### 1. Apply the plugin

In your app's `build.gradle.kts` plugins:

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
    android:value="12345678-1234-1234-1234-123456789012" />
```

However, you will still see the placeholder in your app's android manifest file.

## Reading the UUID in your app

```kotlin
val packageManager = packageManager
val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
val uuid = applicationInfo.metaData?.getString("io.honeycomb.proguard.uuid")
Log.d("ProGuard", "Build UUID: $uuid")
```

## Use case

When your app crashes in production, you can:

1. Read the UUID from the crash report
2. Find the corresponding ProGuard mapping file: `mapping-{uuid}.txt`
3. Deobfuscate the stack trace with the exact mapping used for that build
