package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.FamilyRole
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.repository.WalletRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class JoinFamilyGroupUseCase @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository,
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
            ensureLocalFamilyWallet(
                ownerId = user.uid,
                familyId = family.id,
                familyName = family.name,
            )
            Result.success(family)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * MVP: if the shared family wallet is not on this device yet (multi-device sync later),
     * create a local FAMILY wallet linked to the same [familyId] so Insights can work.
     */
    private suspend fun ensureLocalFamilyWallet(
        ownerId: String,
        familyId: String,
        familyName: String,
    ) {
        val existing =
            walletRepository.observeWalletsByType(WalletType.FAMILY).first()
                .any { it.familyId == familyId }
        if (existing) return

        walletRepository.createWallet(
            Wallet(
                id = UUID.randomUUID().toString(),
                ownerId = ownerId,
                familyId = familyId,
                name = "Dompet $familyName",
                type = WalletType.FAMILY,
                balance = 0L,
                currency = "IDR",
                syncStatus = SyncStatus.PENDING,
                createdAt = Instant.now(),
            ),
        )
    }
}
