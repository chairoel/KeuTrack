package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.FamilyRole
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.repository.WalletRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Leaves the current family:
 * - Member → arrayRemove from memberIds
 * - Sole owner → delete family group doc
 * - Owner with other members → blocked (no ownership transfer in Phase 7)
 *
 * Then clears User.familyId/familyRole and removes local FAMILY wallets for that family.
 * Does not delete the remote shared wallet (other members may still use it).
 */
class LeaveFamilyGroupUseCase @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        val user = userRepository.getCurrentUser().first()
            ?: return Result.failure(IllegalStateException("User belum masuk"))
        val familyId = user.familyId?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Anda belum bergabung dengan keluarga"))

        return try {
            val family = familyRepository.getFamilyById(familyId)
                ?: return Result.failure(IllegalStateException("Data keluarga tidak ditemukan"))

            val isOwner =
                family.ownerId == user.uid ||
                    FamilyRole.fromValue(user.familyRole.orEmpty()) == FamilyRole.OWNER
            val otherMembers = family.memberIds.filter { it != user.uid }

            when {
                isOwner && otherMembers.isNotEmpty() ->
                    return Result.failure(
                        IllegalStateException(
                            "Sebagai owner, Anda tidak bisa keluar saat masih ada anggota lain",
                        ),
                    )
                isOwner ->
                    familyRepository.deleteFamily(familyId).getOrElse {
                        return Result.failure(it)
                    }
                else ->
                    familyRepository.removeMember(familyId, user.uid).getOrElse {
                        return Result.failure(it)
                    }
            }

            userRepository.updateFamilyMembership(
                familyId = null,
                familyRole = null,
            ).getOrElse {
                return Result.failure(it)
            }

            clearLocalFamilyWallets(familyId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun clearLocalFamilyWallets(familyId: String) {
        val familyWallets =
            walletRepository.observeWalletsByType(WalletType.FAMILY).first()
                .filter { it.familyId == familyId }
        for (wallet in familyWallets) {
            runCatching { walletRepository.deleteWallet(wallet.id) }
        }
    }
}
