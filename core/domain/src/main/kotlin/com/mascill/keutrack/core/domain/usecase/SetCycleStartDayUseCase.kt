package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.PeriodPreferences
import com.mascill.keutrack.core.domain.repository.PeriodPreferencesRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class SetCycleStartDayUseCase @Inject constructor(
    private val repository: PeriodPreferencesRepository,
) {
    suspend operator fun invoke(day: Int): Result<Unit> {
        if (day !in PeriodPreferences.MIN_CYCLE_START_DAY..PeriodPreferences.MAX_CYCLE_START_DAY) {
            return Result.failure(
                IllegalArgumentException("Hari mulai siklus harus 1–28"),
            )
        }
        return try {
            repository.setCycleStartDay(day)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
