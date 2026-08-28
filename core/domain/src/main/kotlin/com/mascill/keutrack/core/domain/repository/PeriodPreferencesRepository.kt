package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.PeriodPreferences
import kotlinx.coroutines.flow.Flow

interface PeriodPreferencesRepository {
    fun observe(): Flow<PeriodPreferences>

    suspend fun setCycleStartDay(day: Int)
}
