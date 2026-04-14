plugins {
    alias(libs.plugins.keutrack.feature)
}

android {
    namespace = "com.mascill.keutrack.feature.settings"
}

dependencies {
    implementation(libs.androidx.compose.material.icon.extended)
}
