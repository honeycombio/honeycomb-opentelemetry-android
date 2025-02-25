import java.util.Base64

plugins {
    `maven-publish`
    `signing`
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.spotless)
}

android {
    namespace = "io.honeycomb.opentelemetry.android"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        aarMetadata {
            minCompileSdk = 21
        }

        buildConfigField("String", "HONEYCOMB_DISTRO_VERSION", "\"${project.version}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
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
    implementation(libs.androidx.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    // This is required by opentelemetry-android.
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.auto.service.annotations)
    implementation(libs.instrumentation.activity)
    implementation(libs.instrumentation.anr)
    implementation(libs.instrumentation.crash)
    implementation(libs.instrumentation.slowrendering)
    implementation(libs.opentelemetry.android.agent)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.semconv)
    implementation(libs.opentelemetry.semconv.incubating)
    implementation(libs.opentelemetry.exporter.logging.otlp)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(libs.opentelemetry.instrumentation.api)
    implementation(libs.opentelemetry.baggage.processor)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.opentelemetry.api)
    androidTestImplementation(libs.opentelemetry.sdk)
    androidTestImplementation(libs.opentelemetry.android.agent)
}

apply("${project.rootDir}/spotless.gradle")

publishing {
    publications {
        val maven =
            create<MavenPublication>("release") {
                groupId = "io.honeycomb.android"
                artifactId = "honeycomb-opentelemetry-android"
                version = project.version.toString()

                afterEvaluate {
                    from(components["release"])
                }

                pom {
                    name = "Honeycomb OpenTelemetry Distribution for Android"
                    url = "https://github.com/honeycombio/honeycomb-opentelemetry-android"
                    description = "Honeycomb SDK for configuring OpenTelemetry instrumentation"
                    licenses {
                        license {
                            name = "The Apache License, Version 2.0"
                            url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                        }
                    }
                    developers {
                        developer {
                            id = "Honeycomb"
                            name = "Honeycomb"
                            email = "support@honeycomb.io"
                            organization = "Honeycomb"
                            organizationUrl = "https://honeycomb.io"
                        }
                    }
                    scm {
                        url = "https://github.com/honeycombio/honeycomb-opentelemetry-android"
                        connection = "scm:git:git@github.com:honeycombio/honeycomb-opentelemetry-android.git"
                        developerConnection = "scm:git:git@github.com:honeycombio/honeycomb-opentelemetry-android.git"
                    }
                }
            }

        signing {
            val base64key = System.getenv("GPG_BASE64")
            val pw = System.getenv("GPG_PASSPHRASE")
            val key =
                if (base64key != null && base64key != "") {
                    String(Base64.getDecoder().decode(base64key)).trim()
                } else {
                    ""
                }

            useInMemoryPgpKeys(key, pw)
            sign(maven)
        }
    }
}
