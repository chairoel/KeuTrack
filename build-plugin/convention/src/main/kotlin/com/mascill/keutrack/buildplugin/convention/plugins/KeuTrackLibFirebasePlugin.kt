package com.mascill.keutrack.buildplugin.convention.plugins

import com.mascill.keutrack.buildplugin.convention.utils.configureFirebaseBase
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Adds base Firebase dependencies for Android library modules.
 */
class KeuTrackLibFirebasePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configurations.configureEach {
                exclude(
                    mapOf(
                        "group" to "com.google.firebase",
                        "module" to "protolite-well-known-types",
                    ),
                )
            }
            configureFirebaseBase()
        }
    }
}
