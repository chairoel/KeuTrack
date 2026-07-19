package com.mascill.keutrack.core.data.datasource

import com.mascill.keutrack.core.data.model.AuthUserResponse

interface AuthNetworkDataSource {
    fun getCurrentUser(): AuthUserResponse?
    suspend fun signInWithGoogle(idToken: String): AuthUserResponse?
    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
    ): AuthUserResponse?

    suspend fun signInWithEmail(
        email: String,
        password: String,
    ): AuthUserResponse?

    suspend fun signOut()
}