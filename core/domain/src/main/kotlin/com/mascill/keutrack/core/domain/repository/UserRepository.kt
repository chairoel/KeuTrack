package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.AuthResult
import com.mascill.keutrack.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUser(): Flow<User?>
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun signOut()
    suspend fun syncUserProfile()
}
