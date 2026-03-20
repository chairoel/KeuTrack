package com.mascill.keutrack.buildplugin.convention.plugins

import com.android.build.api.dsl.ApplicationExtension
import com.mascill.keutrack.buildplugin.convention.utils.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * A subclass of [Plugin] to handle compose gradle configuration for android application
 */
class KeuTrackAppComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            extensions.configure<ApplicationExtension> {
                configureAndroidCompose(this@configure)
            }
        }
    }
}