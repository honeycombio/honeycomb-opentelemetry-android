package io.honeycomb.gradle

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.kotlin.dsl.findByType
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.UUID

/**
 * Simple plugin that sets manifestPlaceholders for ProGuard UUID.
 */
class HoneycombProguardUuidPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val projectUuid = UUID.randomUUID().toString()

        // Wait for Android plugin to be applied, then set the placeholder
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
            appExtension.defaultConfig.manifestPlaceholders["PLACEHOLDER_UUID"] = uuid
        }

        if (libExtension != null) {
            libExtension.defaultConfig.manifestPlaceholders["PLACEHOLDER_UUID"] = uuid
        }
    }
}
