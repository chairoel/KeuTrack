plugins {
    alias(libs.plugins.keutrack.library)
    alias(libs.plugins.keutrack.hilt)
    alias(libs.plugins.keutrack.firebase)
}

android {
    namespace = "com.mascill.keutrack.core.data"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.network)
}