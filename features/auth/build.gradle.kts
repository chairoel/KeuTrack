plugins {
    alias(libs.plugins.keutrack.feature)
}

android {
    namespace = "com.mascill.keutrack.feature.auth"
}

dependencies {
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
}