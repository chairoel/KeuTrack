package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.PeriodTotals
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

class GetPeriodTotalsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    data class Params(
        val walletId: String? = null,
        val familyId: String? = null,
        val startDate: Instant? = null,
        val endDate: Instant? = null,
    )

    operator fun invoke(params: Params = Params()): Flow<PeriodTotals> =
        transactionRepository.observePeriodTotals(
            walletId = params.walletId,
            familyId = params.familyId,
            startDate = params.startDate,
            endDate = params.endDate,
        )
}
