package com.mascill.keutrack.core.data.mapper

import com.mascill.keutrack.core.domain.model.PeriodPreferences
import com.mascill.keutrack.core.datastore.PeriodPreferences as PeriodPreferencesProto

class PeriodPreferencesProtoMapper {

    fun toDomain(proto: PeriodPreferencesProto): PeriodPreferences {
        val day = proto.cycleStartDay
        return PeriodPreferences(
            cycleStartDay =
                if (day in PeriodPreferences.MIN_CYCLE_START_DAY..PeriodPreferences.MAX_CYCLE_START_DAY) {
                    day
                } else {
                    PeriodPreferences.DEFAULT_CYCLE_START_DAY
                },
        )
    }

    fun toProto(preferences: PeriodPreferences): PeriodPreferencesProto =
        PeriodPreferencesProto.newBuilder()
            .setCycleStartDay(preferences.cycleStartDay)
            .build()
}
