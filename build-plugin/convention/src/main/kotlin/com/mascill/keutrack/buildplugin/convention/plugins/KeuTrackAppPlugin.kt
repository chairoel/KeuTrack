package com.mascill.keutrack.buildplugin.convention.plugins

import com.android.build.api.dsl.ApplicationExtension
import com.mascill.keutrack.buildplugin.convention.utils.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * A subclass of [Plugin] to handle default gradle configuration for android application
 */
class KeuTrackAppPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            setAppPlugin()
            setAppAndroidConfig()
        }
    }

    private fun Project.setAppPlugin() {
        apply(plugin = "com.android.application")
        apply(plugin = "org.jetbrains.kotlin.android")
    }

    private fun Project.setAppAndroidConfig() {
        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this@configure)
        }
    }
}