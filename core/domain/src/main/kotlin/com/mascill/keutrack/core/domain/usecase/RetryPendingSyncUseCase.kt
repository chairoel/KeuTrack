package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.repository.SyncRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * On screen open: enqueue a sync only when Room still has PENDING/FAILED items.
 * No-ops when everything is already SYNCED.
 */
class RetryPendingSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {
    suspend operator fun invoke() {
        try {
            if (!syncRepository.hasPendingSync()) return
            syncRepository.enqueuePendingSync(force = true)
        } catch (e: CancellationException) {
            throw e
        }
    }
}
