import com.mascill.keutrack.buildplugin.convention.utils.BuildAndroidConfig
import com.mascill.keutrack.buildplugin.convention.utils.BuildTypeDebug
import com.mascill.keutrack.buildplugin.convention.utils.BuildTypeRelease
import com.mascill.keutrack.buildplugin.convention.utils.getLocalProperty

plugins {
    alias(libs.plugins.keutrack.application)
    alias(libs.plugins.keutrack.app.compose)
    alias(libs.plugins.keutrack.app.flavor)
    alias(libs.plugins.keutrack.hilt)
    alias(libs.plugins.keutrack.firebase.app)
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

    signingConfigs {
        create(BuildTypeRelease.type) {
            keyAlias = getLocalProperty("signing_key_alias")
            keyPassword = getLocalProperty("signing_key_password")
            storeFile = file(getLocalProperty("signing_store_file"))
            storePassword = getLocalProperty("signing_store_password")
        }
    }

    buildTypes {
        getByName(BuildTypeRelease.type) {
            isMinifyEnabled = BuildTypeRelease.isMinifyEnabled
            isDebuggable = BuildTypeRelease.isDebuggable
            signingConfig = signingConfigs.getByName(name)
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
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.network)
    implementation(projects.features.auth)
    implementation(projects.features.home)
    implementation(projects.features.splashscreen)
    // --------------------------------------------------------------------------------------------

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.navigation)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}