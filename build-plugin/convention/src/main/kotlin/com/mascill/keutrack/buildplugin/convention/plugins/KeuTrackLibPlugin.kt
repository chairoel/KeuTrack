package com.mascill.keutrack.buildplugin.convention.plugins

import com.android.build.gradle.LibraryExtension
import com.mascill.keutrack.buildplugin.convention.utils.configureFlavors
import com.mascill.keutrack.buildplugin.convention.utils.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * A subclass of [Plugin] to handle all project library module plugin / gradle configuration
 */
class KeuTrackLibPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            setLibPlugin()
            setLibAndroidConfig()
        }
    }

    private fun Project.setLibPlugin() {
        apply(plugin = "com.android.library")
        apply(plugin = "org.jetbrains.kotlin.android")
    }

    private fun Project.setLibAndroidConfig() {
        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this@configure)
            configureFlavors(this@configure)
        }
    }
}