import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

// Configure the build-logic plugins to target JDK 17
// This matches the JDK used to build the project, and is not related to what is running on device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    /**
     * KeuTrack Mikrotrans application plugin
     */
    plugins.register("keutrackAppPlugin") {
        id = libs.plugins.keutrack.application.get().pluginId
        implementationClass = "com.mascill.keutrack.buildplugin.convention.plugins.KeuTrackAppPlugin"
    }

    /**
     * KeuTrack Mikrotrans application compose plugin
     */
    plugins.register("keutrackAppComposePlugin") {
        id = libs.plugins.keutrack.app.compose.get().pluginId
        implementationClass = "com.mascill.keutrack.buildplugin.convention.plugins.KeuTrackAppComposePlugin"
    }

    /**
     * KeuTrack Mikrotrans application flavor plugin
     */
    plugins.register("keutrackAppFlavorPlugin") {
        id = libs.plugins.keutrack.app.flavor.get().pluginId
        implementationClass = "com.mascill.keutrack.buildplugin.convention.plugins.KeuTrackAppFlavorPlugin"
    }

    /**
     * KeuTrack Mikrotrans feature plugin
     */
    plugins.register("keutrackFeaturePlugin") {
        id = libs.plugins.keutrack.feature.get().pluginId
        implementationClass = "com.mascill.keutrack.buildplugin.convention.plugins.KeuTrackFeaturePlugin"
    }

    /**
     * KeuTrack Mikrotrans library plugin
     */
    plugins.register("keutrackLibPlugin") {
        id = libs.plugins.keutrack.library.get().pluginId
        implementationClass = "com.mascill.keutrack.buildplugin.convention.plugins.KeuTrackLibPlugin"
    }

    /**
     * KeuTrack Mikrotrans library compose plugin
     */
    plugins.register("keutrackLibComposePlugin") {
        id = libs.plugins.keutrack.lib.compose.get().pluginId
        implementationClass = "com.mascill.keutrack.buildplugin.convention.plugins.KeuTrackLibComposePlugin"
    }
}