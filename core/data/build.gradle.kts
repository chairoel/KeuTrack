plugins {
    alias(libs.plugins.keutrack.library)
    alias(libs.plugins.keutrack.hilt)
    alias(libs.plugins.keutrack.lib.firebase)
}

android {
    namespace = "com.mascill.keutrack.core.data"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.domain)
    implementation(projects.core.network)

    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Phase 2 — Room + WorkManager offline sync
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    api(libs.androidx.work.runtime.ktx)
    api(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.moshi.kotlin)
    implementation(libs.kotlinx.coroutines.play.services)
}
