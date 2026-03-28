plugins {
    alias(libs.plugins.keutrack.library)
    alias(libs.plugins.keutrack.hilt)
}

android {
    namespace = "com.mascill.keutrack.core.data"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.network)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
}