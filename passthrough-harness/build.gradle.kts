// EXPERIMENT: a JVM-only (Robolectric) harness that runs the same scenario against different SDK
// setups and writes what reached a local OTLP sink to build/harness/<setup>.jsonl.
//
//   ./gradlew :passthrough-harness:testDebugUnitTest -Psetup=current|upstream|shaped
//
// -PwithInteraction puts this repo's :interaction module on the classpath (it does not compile
// against upstream 1.8.0). -PwithClicks puts upstream's view-click instrumentation on it.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.spotless)
}

android {
    namespace = "io.honeycomb.passthrough.harness"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }
    kotlin {
        jvmToolchain(21)
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.fragment)
    implementation(libs.opentelemetry.android.agent)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.sdk)
    // Opt-in upstream click instrumentation (discovered through ServiceLoader once on the classpath).
    if (providers.gradleProperty("withClicks").isPresent) {
        runtimeOnly(libs.opentelemetry.android.view.click)
    }
    if (providers.gradleProperty("withInteraction").isPresent) {
        runtimeOnly(project(":interaction"))
    }

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.opentelemetry.proto)
    testImplementation(libs.protobuf.java.util)
}

tasks.withType<Test>().configureEach {
    systemProperty("harness.setup", providers.gradleProperty("setup").orElse("current").get())
    systemProperty("harness.anr", providers.gradleProperty("anr").orElse("true").get())
    systemProperty(
        "harness.out",
        layout.buildDirectory
            .dir("harness")
            .get()
            .asFile.absolutePath,
    )
    // The ANR scenario blocks the main thread for several seconds.
    testLogging.showStandardStreams = true
}

apply("${project.rootDir}/spotless.gradle")
