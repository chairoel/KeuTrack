package com.mascill.keutrack.core.data.repository

import com.mascill.keutrack.core.data.datasource.UserProfileLocalDataSource
import com.mascill.keutrack.core.data.datasource.firestore.FamilyGroupFirestoreDataSource
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FamilyRepositoryImpl @Inject constructor(
    private val remote: FamilyGroupFirestoreDataSource,
    private val userProfileLocalDataSource: UserProfileLocalDataSource,
) : FamilyRepository {

    override fun observeCurrentFamily(): Flow<FamilyGroup?> =
        userProfileLocalDataSource.observeSignedInUser()
            .flatMapLatest { user ->
                val familyId = user?.familyId
                if (familyId.isNullOrBlank()) {
                    flowOf(null)
                } else {
                    remote.observeFamily(familyId)
                }
            }

    override suspend fun createFamily(name: String, ownerId: String): Result<FamilyGroup> {
        return try {
            val family =
                FamilyGroup(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    inviteCode = generateInviteCode(),
                    ownerId = ownerId,
                    memberIds = listOf(ownerId),
                    createdAt = Instant.now(),
                )
            remote.createFamily(family)
            Result.success(family)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinFamily(inviteCode: String, userId: String): Result<FamilyGroup> {
        return try {
            val normalized = inviteCode.trim().uppercase()
            val existing = remote.findByInviteCode(normalized)
                ?: return Result.failure(IllegalArgumentException("Kode undangan tidak ditemukan"))
            if (existing.memberIds.contains(userId)) {
                return Result.success(existing)
            }
            remote.addMember(existing.id, userId)
            val updated = remote.getFamilyById(existing.id) ?: existing.copy(
                memberIds = existing.memberIds + userId,
            )
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFamilyById(familyId: String): FamilyGroup? {
        return try {
            remote.getFamilyById(familyId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun removeMember(familyId: String, userId: String): Result<Unit> {
        return try {
            remote.removeMember(familyId, userId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFamily(familyId: String): Result<Unit> {
        return try {
            remote.deleteFamily(familyId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        fun chunk(n: Int) =
            buildString {
                repeat(n) { append(alphabet[Random.nextInt(alphabet.length)]) }
            }
        return "KEU-${chunk(3)}-${chunk(3)}"
    }
}
