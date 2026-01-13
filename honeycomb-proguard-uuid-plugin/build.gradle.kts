import java.util.Base64

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    signing
}

group = "io.honeycomb.android"
version = project.version.toString()

dependencies {
    compileOnly(gradleApi())
    compileOnly("com.android.tools.build:gradle:8.13.2")
}

gradlePlugin {
    website = "https://github.com/honeycombio/honeycomb-opentelemetry-android"
    vcsUrl = "https://github.com/honeycombio/honeycomb-opentelemetry-android"

    plugins {
        create("honeycombProguardUuidPlugin") {
            id = "io.honeycomb.proguard-uuid"
            implementationClass = "io.honeycomb.gradle.HoneycombProguardUuidPlugin"
            displayName = "Honeycomb ProGuard UUID Plugin"
            description = "Automatically injects unique UUIDs into Android manifests for ProGuard mapping correlation with Honeycomb observability"
            tags = listOf("honeycomb", "proguard", "android", "observability", "crash-reporting", "mapping")
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        configureEach {
            if (this is MavenPublication) {
                pom {
                    name.set("Honeycomb ProGuard UUID Plugin")
                    url.set("https://github.com/honeycombio/honeycomb-opentelemetry-android")
                    description.set("Automatically injects unique UUIDs into Android manifests for ProGuard mapping correlation")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("Honeycomb")
                            name.set("Honeycomb")
                            email.set("support@honeycomb.io")
                            organization.set("Honeycomb")
                            organizationUrl.set("https://honeycomb.io")
                        }
                    }

                    scm {
                        url.set("https://github.com/honeycombio/honeycomb-opentelemetry-android")
                        connection.set("scm:git:git@github.com:honeycombio/honeycomb-opentelemetry-android.git")
                        developerConnection.set("scm:git:git@github.com:honeycombio/honeycomb-opentelemetry-android.git")
                    }
                }
            }
        }
    }
}

signing {
    val isDevBuild = System.getenv("GPG_BASE64") == null
    if (!isDevBuild) {
        val base64key = System.getenv("GPG_BASE64")
        val pw = System.getenv("GPG_PASSPHRASE")
        val key = if (base64key != null && base64key != "") {
            String(Base64.getDecoder().decode(base64key)).trim()
        } else {
            ""
        }

        useInMemoryPgpKeys(key, pw)
        sign(publishing.publications)
    }
}
