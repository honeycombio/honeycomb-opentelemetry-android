// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.publish.plugin)
}

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getenv("OSSRH_TOKEN_USERNAME"))
            password.set(System.getenv("OSSRH_TOKEN_PASSWORD"))
        }
    }
}
