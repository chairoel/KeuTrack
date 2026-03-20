package com.mascill.keutrack.buildplugin.convention.plugins

import com.android.build.gradle.LibraryExtension
import com.mascill.keutrack.buildplugin.convention.utils.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * A subclass of [Plugin] to handle compose gradle configuration for android library
 */
class KeuTrackLibComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            extensions.configure<LibraryExtension> {
                configureAndroidCompose(this@configure)
            }
        }
    }
}