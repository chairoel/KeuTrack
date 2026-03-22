import com.mascill.keutrack.buildplugin.convention.utils.BuildAndroidConfig
import com.mascill.keutrack.buildplugin.convention.utils.BuildTypeDebug
import com.mascill.keutrack.buildplugin.convention.utils.BuildTypeRelease

plugins {
    alias(libs.plugins.keutrack.application)
    alias(libs.plugins.keutrack.app.compose)
    alias(libs.plugins.keutrack.app.flavor)
    alias(libs.plugins.keutrack.hilt)
}


android {
    namespace = "com.mascill.keutrack"
    compileSdk = BuildAndroidConfig.COMPILE_SDK_VERSION

    defaultConfig {
        applicationId = BuildAndroidConfig.APPLICATION_ID
        targetSdk = BuildAndroidConfig.TARGET_SDK_VERSION
        versionCode = BuildAndroidConfig.VERSION_CODE
        versionName = BuildAndroidConfig.VERSION_NAME
    }

    buildTypes {
        getByName(BuildTypeRelease.type) {
            isMinifyEnabled = BuildTypeRelease.isMinifyEnabled
            isDebuggable = BuildTypeRelease.isDebuggable
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName(BuildTypeDebug.type) {
            versionNameSuffix = BuildTypeDebug.VERSION_NAME_SUFFIX
            isMinifyEnabled = BuildTypeDebug.isMinifyEnabled
        }
    }
}

dependencies {
    // internal modules ---------------------------------------------------------------------------
    implementation(projects.core.designsystem)
    // --------------------------------------------------------------------------------------------

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}