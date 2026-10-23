plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.spotless)
}

// A minimal app that, unlike :example, does NOT use core library desugaring. It exists so that
// the emulator smoke test catches Java APIs that the device does not have, such as the
// Duration.toSeconds() call (missing before Android 12) in the upstream slow rendering
// instrumentation.
//
// minSdk is 26 on purpose: upstream only requires desugaring below API 26, so this is the
// configuration it documents as not needing it, and nothing else should break without it.
android {
    namespace = "io.honeycomb.opentelemetry.android.example.nodesugar"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.honeycomb.opentelemetry.android.example.nodesugar"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        // Deliberately not enabling isCoreLibraryDesugaringEnabled.
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // This is where the upstream slow rendering instrumentation comes from.
    implementation(libs.opentelemetry.android.agent)
    implementation(project(":core"))

    androidTestImplementation(libs.androidx.junit)
}

apply("${project.rootDir}/spotless.gradle")
