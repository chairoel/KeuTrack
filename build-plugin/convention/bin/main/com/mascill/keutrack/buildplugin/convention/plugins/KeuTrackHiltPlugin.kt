package com.mascill.keutrack.buildplugin.convention.plugins

import com.mascill.keutrack.buildplugin.convention.utils.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * A subclass of [Plugin] to handle all project module hilt configuration
 */
class KeuTrackHiltPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.dagger.hilt.android")
            apply(plugin = "com.google.devtools.ksp")

            dependencies {
                add("implementation", libs.findLibrary("hilt.android").get())
                add("ksp", libs.findLibrary("hilt.compiler").get())

                // integrating hilt with navigation compose
                add("implementation", libs.findLibrary("androidx.hilt.navigation.compose").get())
            }
        }
    }
}