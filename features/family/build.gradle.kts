plugins {
    alias(libs.plugins.keutrack.feature)
}

android {
    namespace = "com.mascill.keutrack.feature.family"
}

dependencies {
    implementation(libs.androidx.compose.material.icon.extended)
}
