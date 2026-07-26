package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.FamilyRole
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.repository.WalletRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class JoinFamilyGroupUseCase @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository,
    private val syncFamilyData: SyncFamilyDataUseCase,
) {
    suspend operator fun invoke(inviteCode: String): Result<FamilyGroup> {
        val code = inviteCode.trim().uppercase()
        if (code.isBlank()) {
            return Result.failure(IllegalArgumentException("Kode undangan tidak boleh kosong"))
        }

        val user = userRepository.getCurrentUser().first()
            ?: return Result.failure(IllegalStateException("User belum masuk"))
        if (!user.familyId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Anda sudah bergabung dengan keluarga"))
        }

        return try {
            val family = familyRepository.joinFamily(code, user.uid).getOrElse {
                return Result.failure(it)
            }
            userRepository.updateFamilyMembership(
                familyId = family.id,
                familyRole = FamilyRole.MEMBER.value,
            ).getOrElse {
                return Result.failure(it)
            }
            ensureSharedFamilyWallet(familyId = family.id)
            Result.success(family)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pull the canonical family wallet from Firestore into Room.
     * Retries briefly if the owner wallet is not remote yet — does **not** mint a second UUID
     * (that caused split-brain History where A never saw B's txs on W_B).
     */
    private suspend fun ensureSharedFamilyWallet(familyId: String) {
        repeat(PULL_ATTEMPTS) { attempt ->
            syncFamilyData(familyId)
            val hasFamilyWallet =
                walletRepository.observeWalletsByType(WalletType.FAMILY).first()
                    .any { it.familyId == familyId }
            if (hasFamilyWallet) return
            if (attempt < PULL_ATTEMPTS - 1) {
                delay(PULL_RETRY_DELAY_MS)
            }
        }
        // Membership is already saved; Family tab pull can hydrate the wallet later.
    }

    private companion object {
        const val PULL_ATTEMPTS = 4
        const val PULL_RETRY_DELAY_MS = 1_000L
    }
}
