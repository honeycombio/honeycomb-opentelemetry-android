package io.honeycomb.gradle

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.kotlin.dsl.findByType
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.util.UUID

/**
 * Simple plugin that sets manifestPlaceholders for ProGuard UUID.
 */
class HoneycombProguardUuidPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val projectUuid = UUID.randomUUID().toString()
        val uuidPropertiesOutputFile = project.layout.buildDirectory
            .file("generated/honeycomb/proguard-uuid.properties")
            .get()
            .asFile

        // Write the generated UUID to a properties file
        uuidPropertiesOutputFile.parentFile?.mkdirs()
        uuidPropertiesOutputFile.writeText("io.honeycomb.proguard.uuid=$projectUuid")

        // Update manifest placeholders to the generated UUID
        project.plugins.withId("com.android.application") {
            setPlaceholder(project, projectUuid)
        }
        project.plugins.withId("com.android.library") {
            setPlaceholder(project, projectUuid)
        }
    }

    private fun setPlaceholder(project: Project, uuid: String) {
        val appExtension = project.extensions.findByType<ApplicationExtension>()
        val libExtension = project.extensions.findByType<LibraryExtension>()

        if (appExtension != null) {
            appExtension.defaultConfig.manifestPlaceholders["PROGUARD_UUID"] = uuid
        }

        if (libExtension != null) {
            libExtension.defaultConfig.manifestPlaceholders["PROGUARD_UUID"] = uuid
        }
    }
}
