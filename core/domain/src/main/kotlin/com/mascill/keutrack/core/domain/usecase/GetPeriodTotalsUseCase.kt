package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.PeriodTotals
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

class GetPeriodTotalsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(
        startDate: Instant,
        endDate: Instant,
    ): Flow<PeriodTotals> =
        transactionRepository.observePeriodTotals(
            startDate = startDate,
            endDate = endDate,
        )
}
