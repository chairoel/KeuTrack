package com.mascill.keutrack.buildplugin.convention.plugins

import com.mascill.keutrack.buildplugin.convention.utils.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * A subclass of [Plugin] to handle Firebase dependencies for both app and library modules.
 * Applies Google Services only when the Android application plugin is present.
 */
class KeuTrackFirebasePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.application") {
                apply(plugin = "com.google.gms.google-services")
            }

            dependencies {
                add("implementation", platform(libs.findLibrary("firebase-bom").get()))
                // Exclude protolite-well-known-types: it duplicates classes from protobuf-javalite
                // (pulled by :core:datastore via protobuf-kotlin-lite), causing checkDuplicateClasses to fail.
                "implementation"(libs.findLibrary("firebase-auth").get()) {
                    exclude(
                        mapOf(
                            "group" to "com.google.firebase",
                            "module" to "protolite-well-known-types",
                        ),
                    )
                }
                "implementation"(libs.findLibrary("firebase-firestore").get()) {
                    exclude(
                        mapOf(
                            "group" to "com.google.firebase",
                            "module" to "protolite-well-known-types",
                        ),
                    )
                }
                add("implementation", libs.findLibrary("kotlinx-coroutines-play-services").get())
            }
        }
    }
}
