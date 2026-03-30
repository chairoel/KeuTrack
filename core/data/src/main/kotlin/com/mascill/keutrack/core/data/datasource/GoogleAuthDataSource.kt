package com.mascill.keutrack.core.data.datasource

interface GoogleAuthDataSource {
    suspend fun getGoogleIdToken(): String
}
