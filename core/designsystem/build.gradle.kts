plugins {
    alias(libs.plugins.keutrack.library)
    alias(libs.plugins.keutrack.lib.compose)
}
android {
    namespace = "com.mascill.keutrack.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.material)
}