package com.mascill.keutrack.core.data.repository

import com.mascill.keutrack.core.data.datasource.PeriodPreferencesLocalDataSource
import com.mascill.keutrack.core.domain.model.PeriodPreferences
import com.mascill.keutrack.core.domain.repository.PeriodPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeriodPreferencesRepositoryImpl @Inject constructor(
    private val local: PeriodPreferencesLocalDataSource,
) : PeriodPreferencesRepository {

    override fun observe(): Flow<PeriodPreferences> = local.observe()

    override suspend fun setCycleStartDay(day: Int) {
        local.setCycleStartDay(day)
    }
}
