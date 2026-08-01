package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.FamilyRole
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.SyncRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.repository.WalletRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class CreateFamilyGroupUseCase @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository,
    private val syncRepository: SyncRepository,
) {
    suspend operator fun invoke(name: String): Result<FamilyGroup> {
        val trimmed = name.trim()
        if (trimmed.length < MIN_NAME_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Nama keluarga minimal $MIN_NAME_LENGTH karakter"),
            )
        }
        if (trimmed.length > MAX_NAME_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Nama keluarga maksimal $MAX_NAME_LENGTH karakter"),
            )
        }

        val user = userRepository.getCurrentUser().first()
            ?: return Result.failure(IllegalStateException("User belum masuk"))
        if (!user.familyId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Anda sudah bergabung dengan keluarga"))
        }

        return try {
            val family = familyRepository.createFamily(trimmed, user.uid).getOrElse {
                return Result.failure(it)
            }
            userRepository.updateFamilyMembership(
                familyId = family.id,
                familyRole = FamilyRole.OWNER.value,
            ).getOrElse {
                return Result.failure(it)
            }
            ensureFamilyWallet(
                ownerId = user.uid,
                familyId = family.id,
                familyName = family.name,
            )
            // Await wallet push so joiners can pull the same canonical id (avoid W_B mint race).
            try {
                syncRepository.syncPendingWallets()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                syncRepository.enqueuePendingSync(force = true)
            }
            Result.success(family)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun ensureFamilyWallet(
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

    private companion object {
        const val MIN_NAME_LENGTH = 2
        const val MAX_NAME_LENGTH = 40
    }
}
