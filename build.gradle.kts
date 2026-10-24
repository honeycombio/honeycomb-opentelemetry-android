// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.publish.plugin)
    alias(libs.plugins.compose.compiler) apply false
}

var isDevBuild by extra { true }

allprojects {
    val tag: String? = System.getenv("CIRCLE_TAG")
    if (tag != null && tag.startsWith("v")) {
        version = tag.slice(1 until tag.length)
        isDevBuild = false
    } else {
        version = "0.0.0-DEVELOPMENT"
    }
}

nexusPublishing {
    repositories {
        // see https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#configuration
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_PASSWORD"))
        }
    }
}


// EXPERIMENT: upstream 1.8.0 pulls kotlin-stdlib 2.4.x transitively; pin it back so the
// project's Kotlin 2.1.20 compiler can still read the metadata.
allprojects {
    configurations.all {
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
        // upstream 1.8.0 constrains androidx.core to 1.19.1, which needs AGP 9.1+ and compileSdk 37.
        resolutionStrategy.force("androidx.core:core:1.17.0", "androidx.core:core-ktx:1.17.0")
        // ...and the same for lifecycle-runtime-compose and okhttp (both want compileSdk 37 / AGP 9.1+).
        resolutionStrategy.eachDependency {
            if (requested.group == "androidx.lifecycle") useVersion("2.9.4")
            if (requested.group == "com.squareup.okhttp3") useVersion("5.3.2")
        }
    }
}
