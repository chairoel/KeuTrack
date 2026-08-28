package com.mascill.keutrack.core.data.datasource

import androidx.datastore.core.DataStore
import com.mascill.keutrack.core.data.mapper.PeriodPreferencesProtoMapper
import com.mascill.keutrack.core.domain.model.PeriodPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.mascill.keutrack.core.datastore.PeriodPreferences as PeriodPreferencesProto

class PeriodPreferencesLocalDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<PeriodPreferencesProto>,
    private val mapper: PeriodPreferencesProtoMapper,
) : PeriodPreferencesLocalDataSource {

    override fun observe(): Flow<PeriodPreferences> =
        dataStore.data.map(mapper::toDomain)

    override suspend fun setCycleStartDay(day: Int) {
        dataStore.updateData { current ->
            current.toBuilder().setCycleStartDay(day).build()
        }
    }
}
