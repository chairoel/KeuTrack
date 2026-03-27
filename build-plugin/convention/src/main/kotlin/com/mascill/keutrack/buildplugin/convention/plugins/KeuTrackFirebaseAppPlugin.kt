package com.mascill.keutrack.buildplugin.convention.plugins

import com.mascill.keutrack.buildplugin.convention.utils.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * A subclass of [Plugin] to handle Firebase configuration for the :app module only.
 * This plugin applies the Google Services plugin (required to process google-services.json)
 * alongside all Firebase dependencies and Credentials API for Google Sign-In.
 *
 * Use [KeuTrackFirebasePlugin] for library modules that only need Firebase SDK dependencies.
 */
class KeuTrackFirebaseAppPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.gms.google-services")

            dependencies {
                add("implementation", platform(libs.findLibrary("firebase-bom").get()))
                add("implementation", libs.findLibrary("firebase-auth").get())
                add("implementation", libs.findLibrary("firebase-firestore").get())
                add("implementation", libs.findLibrary("kotlinx-coroutines-play-services").get())
                add("implementation", libs.findLibrary("androidx-credentials").get())
                add("implementation", libs.findLibrary("androidx-credentials-play-services-auth").get())
                add("implementation", libs.findLibrary("googleid").get())
            }
        }
    }
}
