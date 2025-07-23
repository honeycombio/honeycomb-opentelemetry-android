plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "io.honeycomb"
version = "0.0.1"

dependencies {
    implementation("com.android.tools.build:gradle:8.11.1")
}

gradlePlugin {
    plugins {
        create("honeycombProguardUuidPlugin") {
            id = "io.honeycomb.proguard-uuid"
            implementationClass = "io.honeycomb.gradle.HoneycombProguardUuidPlugin"
            displayName = "Honeycomb ProGuard UUID Plugin"
            description = "A simple plugin for generating ProGuard UUIDs for Honeycomb"
        }
    }
}
