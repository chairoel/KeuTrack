package com.mascill.keutrack.core.data.datasource

import com.mascill.keutrack.core.domain.model.PeriodPreferences
import kotlinx.coroutines.flow.Flow

interface PeriodPreferencesLocalDataSource {
    fun observe(): Flow<PeriodPreferences>

    suspend fun setCycleStartDay(day: Int)
}
