package com.mascill.keutrack.buildplugin.convention.plugins

import com.mascill.keutrack.buildplugin.convention.utils.configureFirebaseBase
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Adds base Firebase dependencies for Android library modules.
 *
 * Do not exclude `protolite-well-known-types`: Firestore needs types such as
 * `com.google.type.LatLng` from that artifact at runtime.
 */
class KeuTrackLibFirebasePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureFirebaseBase()
        }
    }
}
