package com.mascill.keutrack.buildplugin.convention.plugins

import com.android.build.api.dsl.ApplicationExtension
import com.mascill.keutrack.buildplugin.convention.utils.configureFlavors
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * A subclass of [Plugin] to handle flavor gradle configuration for android application
 */
class KeuTrackAppFlavorPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<ApplicationExtension> {
                configureFlavors(this@configure)
            }
        }
    }
}