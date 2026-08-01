package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.FamilyGroup
import kotlinx.coroutines.flow.Flow

interface FamilyRepository {
    fun observeCurrentFamily(): Flow<FamilyGroup?>

    suspend fun createFamily(name: String, ownerId: String): Result<FamilyGroup>

    suspend fun joinFamily(inviteCode: String, userId: String): Result<FamilyGroup>

    suspend fun getFamilyById(familyId: String): FamilyGroup?
}
