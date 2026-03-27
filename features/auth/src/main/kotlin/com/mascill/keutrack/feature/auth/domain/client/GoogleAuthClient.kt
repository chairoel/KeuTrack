package com.mascill.keutrack.feature.auth.domain.client

import android.content.Context

interface GoogleAuthClient {
    suspend fun signIn(context: Context): GoogleSignInResult
}

data class GoogleSignInResult(
    val idToken: String?,
    val error: String?
)
