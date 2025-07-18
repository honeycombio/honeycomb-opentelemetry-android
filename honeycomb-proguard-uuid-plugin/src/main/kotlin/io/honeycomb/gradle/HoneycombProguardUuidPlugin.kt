package io.honeycomb.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.UUID

/**
 * Super simple Honeycomb ProGuard UUID plugin that prints messages.
 */
class HoneycombProguardUuidPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        println("🍯 Honeycomb ProGuard UUID Plugin applied to project: ${project.name}")
        
        // Register a simple task that prints UUID info
        project.tasks.register("printProguardUuid") {
            group = "honeycomb"
            description = "Prints ProGuard UUID information"
            
            doLast {
                val uuid = UUID.randomUUID().toString()
                println("🔑 Generated ProGuard UUID: $uuid")
                println("📦 Project: ${project.name}")
                println("📁 Build directory: ${project.layout.buildDirectory.get()}")
                println("🏗️  This UUID would be used for ProGuard mapping correlation!")
                println("✨ Honeycomb ProGuard UUID Plugin - Simple and Sweet! ✨")
            }
        }
        
        // Also register a simpler hello task
        project.tasks.register("honeycombHello") {
            group = "honeycomb"
            description = "Says hello from Honeycomb"
            
            doLast {
                println("👋 Hello from Honeycomb ProGuard UUID Plugin!")
                println("🍯 Sweet as honey, simple as can be!")
                println("📱 Ready to handle your Android ProGuard UUIDs!")
            }
        }
        
        println("✅ Honeycomb ProGuard UUID Plugin setup complete!")
    }
}
