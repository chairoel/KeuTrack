package com.mascill.keutrack.core.data.repository

import com.mascill.keutrack.core.data.datasource.UserProfileLocalDataSource
import com.mascill.keutrack.core.data.datasource.firestore.FamilyGroupFirestoreDataSource
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
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
                val familyId = user?.familyId?.takeIf { it.isNotBlank() }
                    ?: return@flatMapLatest flowOf(null)
                remote.observeFamily(familyId).onEach { family ->
                    syncOwnMemberName(family, user.uid, resolveDisplayName(user))
                }
            }

    override suspend fun createFamily(name: String, ownerId: String): Result<FamilyGroup> {
        return try {
            val ownerName = resolveDisplayName(userProfileLocalDataSource.getSignedInUser())
            val family =
                FamilyGroup(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    inviteCode = generateInviteCode(),
                    ownerId = ownerId,
                    memberIds = listOf(ownerId),
                    memberNames = ownerName?.let { mapOf(ownerId to it) }.orEmpty(),
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
            val joinerName = resolveDisplayName(userProfileLocalDataSource.getSignedInUser())
            if (joinerName != null) {
                try {
                    remote.upsertMemberName(existing.id, userId, joinerName)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Membership is already saved; name is backfilled on next observe.
                }
            }
            val updated = remote.getFamilyById(existing.id) ?: existing.copy(
                memberIds = existing.memberIds + userId,
                memberNames = if (joinerName != null) {
                    existing.memberNames + (userId to joinerName)
                } else {
                    existing.memberNames
                },
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

    private suspend fun syncOwnMemberName(
        family: FamilyGroup?,
        userId: String,
        displayName: String?,
    ) {
        if (family == null || displayName.isNullOrBlank()) return
        if (family.memberNames[userId] == displayName) return
        try {
            remote.upsertMemberName(family.id, userId, displayName)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort denormalized name; initials still fall back to the local user.
        }
    }

    private fun resolveDisplayName(user: User?): String? {
        val fromDisplay = user?.displayName?.trim().orEmpty()
        if (fromDisplay.isNotEmpty()) return fromDisplay
        val fromEmail = user?.email?.substringBefore('@')?.trim().orEmpty()
        return fromEmail.ifEmpty { null }
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
