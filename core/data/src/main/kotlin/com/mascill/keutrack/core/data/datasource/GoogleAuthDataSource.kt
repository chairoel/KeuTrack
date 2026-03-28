package com.mascill.keutrack.core.data.datasource

import com.mascill.keutrack.core.domain.model.GoogleSignInResult

interface GoogleAuthDataSource {
    suspend fun getGoogleIdToken(): GoogleSignInResult
}
