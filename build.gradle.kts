// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.publish.plugin)
}

allprojects {
    val tag: String? = System.getenv("CIRCLE_TAG")
    version = if (tag != null && tag.startsWith("v")) {
        tag.slice(1 until tag.length)
    } else {
        "0.0.0-DEVELOPMENT"
    }
}

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_PASSWORD"))
        }
    }
}

