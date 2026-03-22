package com.mascill.keutrack.buildplugin.convention.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * A subclass of [Plugin] to handle all project feature module plugin / gradle configuration
 */
class KeuTrackFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            setFeaturePlugin()
            setFeatureDependencies()
        }
    }

    private fun Project.setFeaturePlugin() {
        apply(plugin = "keutrack.library")
        apply(plugin = "keutrack.lib.compose")
        apply(plugin = "keutrack.hilt")
        apply(plugin = "com.google.devtools.ksp")
        apply(plugin = "kotlinx-serialization")
    }

    private fun Project.setFeatureDependencies() {
        dependencies {

        }
    }
}