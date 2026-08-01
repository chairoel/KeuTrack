package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.repository.SyncRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Pull shared family wallet + transactions into Room for the current user's family.
 * No-op when [familyId] is blank and the signed-in user has no `familyId`.
 */
class SyncFamilyDataUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository,
) {
    suspend operator fun invoke(familyId: String? = null): Result<Unit> {
        return try {
            val resolvedFamilyId =
                familyId?.takeIf { it.isNotBlank() }
                    ?: userRepository.getCurrentUser().first()?.familyId
            if (resolvedFamilyId.isNullOrBlank()) {
                return Result.success(Unit)
            }
            syncRepository.syncFamilyData(resolvedFamilyId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
