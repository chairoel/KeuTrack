import com.mascill.keutrack.buildplugin.convention.utils.BuildAndroidConfig

plugins {
    alias(libs.plugins.keutrack.library)
    alias(libs.plugins.keutrack.hilt)
    alias(libs.plugins.keutrack.firebase)
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
    api(libs.retrofit.core)
    api(libs.retrofit.moshi)
    implementation(libs.moshi.kotlin)

    // http network logger
    devImplementation(libs.chucker.debug)
    prodImplementation(libs.chucker.release)
}