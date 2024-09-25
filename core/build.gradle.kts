plugins {
    `maven-publish`
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "io.honeycomb.opentelemetry.android"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        aarMetadata {
            minCompileSdk = 21
        }

        buildConfigField("String","HONEYCOMB_DISTRO_VERSION","\"0.0.1-alpha\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    testOptions {
        managedDevices {
            localDevices {
                create("pixel8api35") {
                    device = "Pixel 8"
                    apiLevel = 35
                    systemImageSource = "google"
                }
            }
        }
    }
}

dependencies {
    // This is required by opentelemetry-android.
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.instrumentation.activity)
    implementation(libs.instrumentation.anr)
    implementation(libs.instrumentation.crash)
    implementation(libs.instrumentation.slowrendering)
    implementation(libs.opentelemetry.android.agent)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(libs.opentelemetry.instrumentation.api)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.opentelemetry.api)
    androidTestImplementation(libs.opentelemetry.sdk)
    androidTestImplementation(libs.opentelemetry.android.agent)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "io.honeycomb.android"
            artifactId = "honeycomb-opentelemetry-android"
            version = "0.0.1-alpha"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}