package com.mascill.keutrack.buildplugin.convention.utils

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureFirebaseBase() {
    dependencies {
        add("implementation", platform(libs.findLibrary("firebase-bom").get()))
        add("implementation", libs.findLibrary("firebase-analytics").get())
        add("implementation", libs.findLibrary("kotlinx-coroutines-play-services").get())
    }
}
