package io.honeycomb.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.util.UUID

/**
 * Super simple Honeycomb ProGuard UUID plugin that injects UUIDs into Android manifests.
 */
class HoneycombProguardUuidPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Generate UUID once for this project
        val projectUuid = UUID.randomUUID().toString()

        // Simple task that replaces placeholder with actual UUID
        val injectTask = project.tasks.register("injectProguardUuid") {
            group = "honeycomb"
            description = "Replaces UUID placeholder with actual UUID in the Android manifest file"

            doLast {
                // Find the main AndroidManifest.xml
                val manifestFile = File(project.projectDir, "src/main/AndroidManifest.xml")

                if (manifestFile.exists()) {
                    var content = manifestFile.readText()

                    // Replace placeholder with actual UUID
                    if (content.contains("HONEYCOMB_PROGUARD_UUID_PLACEHOLDER")) {
                        content = content.replace("HONEYCOMB_PROGUARD_UUID_PLACEHOLDER", projectUuid)
                        manifestFile.writeText(content)
                    } else {
                        println("⚠️ Placeholder 'HONEYCOMB_PROGUARD_UUID_PLACEHOLDER' not found in AndroidManifest.xml")
                    }
                } else {
                    println("⚠️ AndroidManifest.xml not found at: ${manifestFile.absolutePath}")
                }
            }
        }

        project.afterEvaluate {
            project.tasks.findByName("preBuild")?.dependsOn(injectTask)
        }
    }
}
