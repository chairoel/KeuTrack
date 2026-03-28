package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.GoogleSignInResult

interface GoogleAuthRepository {
    suspend fun getGoogleIdToken(): GoogleSignInResult
}
