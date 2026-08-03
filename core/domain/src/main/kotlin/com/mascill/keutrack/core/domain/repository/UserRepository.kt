package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.AuthResult
import com.mascill.keutrack.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUser(): Flow<User?>
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun registerWithEmail(
        fullName: String,
        email: String,
        password: String,
    ): AuthResult

    suspend fun signInWithEmail(
        email: String,
        password: String,
    ): AuthResult

    suspend fun signOut()
    suspend fun syncUserProfile()

    /**
     * Explicit membership write for Phase 6 family create/join.
     * Does not go through auth sign-in upsert paths.
     */
    suspend fun updateFamilyMembership(
        familyId: String?,
        familyRole: String?,
    ): Result<Unit>

    /**
     * Explicit currency preference write for Phase 7 settings.
     * Does not go through auth sign-in upsert paths.
     */
    suspend fun updateCurrency(currency: String): Result<Unit>
}
