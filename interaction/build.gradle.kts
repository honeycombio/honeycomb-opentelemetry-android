import java.util.Base64

plugins {
    `maven-publish`
    `signing`

    alias(libs.plugins.android.library)
    alias(libs.plugins.spotless)

    alias(libs.plugins.ksp)
}

android {
    namespace = "io.honeycomb.opentelemetry.android.interaction"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
        aarMetadata {
            minCompileSdk = 21
        }

        buildConfigField("String", "HONEYCOMB_DISTRO_VERSION", "\"${project.version}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    publishing {
        singleVariant("release")
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }
}

dependencies {
    ksp(libs.auto.service.ksp)
    compileOnly(libs.auto.service.annotations)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.opentelemetry.android.core)
    implementation(libs.opentelemetry.android.instrumentation)
    implementation(libs.opentelemetry.api)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

apply("${project.rootDir}/spotless.gradle")

publishing {
    publications {
        val maven =
            create<MavenPublication>("release") {
                groupId = "io.honeycomb.android"
                artifactId = "honeycomb-opentelemetry-android-interaction"
                version = project.version.toString()

                afterEvaluate {
                    from(components["release"])
                }

                pom {
                    name = "Honeycomb Instrumentation for User Interactions"
                    url = "https://github.com/honeycombio/honeycomb-opentelemetry-android"
                    description = "Honeycomb SDK for instrumenting user interactions in Android applications"
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
            val isDevBuild: Boolean by rootProject.extra
            if (!isDevBuild) {
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
}
