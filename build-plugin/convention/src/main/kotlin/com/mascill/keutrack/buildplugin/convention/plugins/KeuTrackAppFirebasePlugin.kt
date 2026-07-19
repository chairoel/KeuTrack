package com.mascill.keutrack.buildplugin.convention.plugins

import com.mascill.keutrack.buildplugin.convention.utils.configureFirebaseBase
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/**
 * Adds base Firebase dependencies and applies Google Services for Android application modules.
 */
class KeuTrackAppFirebasePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.gms.google-services")
            configureFirebaseBase()
        }
    }
}
