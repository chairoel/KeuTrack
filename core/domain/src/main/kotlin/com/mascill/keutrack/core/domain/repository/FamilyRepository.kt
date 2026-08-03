package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.FamilyGroup
import kotlinx.coroutines.flow.Flow

interface FamilyRepository {
    fun observeCurrentFamily(): Flow<FamilyGroup?>

    suspend fun createFamily(name: String, ownerId: String): Result<FamilyGroup>

    suspend fun joinFamily(inviteCode: String, userId: String): Result<FamilyGroup>

    suspend fun getFamilyById(familyId: String): FamilyGroup?

    /** Remove [userId] from [familyId] memberIds (arrayRemove). */
    suspend fun removeMember(familyId: String, userId: String): Result<Unit>

    /** Delete family group document (owner-only on Firestore rules). */
    suspend fun deleteFamily(familyId: String): Result<Unit>
}
