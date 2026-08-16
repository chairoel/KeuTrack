package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.repository.SyncRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Pull personal wallet + transactions for the signed-in user into Room.
 * No-op if there is no current user.
 */
class SyncPersonalDataUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository,
) {
    suspend operator fun invoke(userId: String? = null): Result<Unit> {
        return try {
            val resolvedUserId =
                userId?.takeIf { it.isNotBlank() }
                    ?: userRepository.getCurrentUser().first()?.uid
            if (resolvedUserId.isNullOrBlank()) {
                return Result.success(Unit)
            }
            syncRepository.syncPersonalData(resolvedUserId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
