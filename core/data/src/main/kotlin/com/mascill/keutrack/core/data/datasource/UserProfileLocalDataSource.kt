package com.mascill.keutrack.core.data.datasource

import com.mascill.keutrack.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserProfileLocalDataSource {
    fun observeSignedInUser(): Flow<User?>
    suspend fun persist(user: User)
    suspend fun clear()
}
