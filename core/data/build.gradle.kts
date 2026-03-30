plugins {
    alias(libs.plugins.keutrack.library)
    alias(libs.plugins.keutrack.hilt)
    alias(libs.plugins.keutrack.firebase.lib)
}

android {
    namespace = "com.mascill.keutrack.core.data"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val googleServerClientId = providers
            .gradleProperty("GOOGLE_SERVER_CLIENT_ID")
            .orElse("100547827166-uqtn9is2df1k931808lm6ff8i79ns988.apps.googleusercontent.com")
            .get()

        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleServerClientId\"")
    }
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.network)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
}