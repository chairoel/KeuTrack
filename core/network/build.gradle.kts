import com.mascill.keutrack.buildplugin.convention.utils.BuildAndroidConfig

plugins {
    alias(libs.plugins.keutrack.library)
    alias(libs.plugins.keutrack.hilt)
}

android {
    namespace = "com.mascill.keutrack.core.network"
    ndkVersion = BuildAndroidConfig.NDK_VERSION

    // Native C NDK configuration
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = BuildAndroidConfig.C_VERSION
        }
    }
}

dependencies {
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)

    // http network logger
    debugImplementation(libs.chucker.debug)
    releaseImplementation(libs.chucker.release)
}