package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUser(): Flow<User?>
    suspend fun syncUserProfile()
}
