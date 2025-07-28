import java.util.Base64

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    signing
}

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

publishing {
    publications {
        val maven =
            create<MavenPublication>("release") {
                groupId = "io.honeycomb.android"
                artifactId = "honeycomb-opentelemetry-android-proguard-uuid-plugin"
                version = project.version.toString()

                afterEvaluate {
                    from(components["java"])
                }

                pom {
                    name = "Honeycomb ProGuard UUID Plugin"
                    url = "https://github.com/honeycombio/honeycomb-opentelemetry-android"
                    description = "Automatically injects unique UUIDs into Android manifests for ProGuard mapping correlation"
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
            val isDevBuild = System.getenv("GPG_BASE64") == null
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
