package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.PeriodPreferences
import com.mascill.keutrack.core.domain.repository.PeriodPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePeriodPreferencesUseCase @Inject constructor(
    private val repository: PeriodPreferencesRepository,
) {
    operator fun invoke(): Flow<PeriodPreferences> = repository.observe()
}
