package com.mascill.keutrack.core.data.mapper

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.PeriodPreferences
import org.junit.Test
import com.mascill.keutrack.core.datastore.PeriodPreferences as PeriodPreferencesProto

class PeriodPreferencesProtoMapperTest {

    private val mapper = PeriodPreferencesProtoMapper()

    @Test
    fun `default proto maps to cycle start day 1`() {
        val domain = mapper.toDomain(PeriodPreferencesProto.getDefaultInstance())

        assertThat(domain.cycleStartDay).isEqualTo(PeriodPreferences.DEFAULT_CYCLE_START_DAY)
    }

    @Test
    fun `out of range proto day falls back to default`() {
        val proto = PeriodPreferencesProto.newBuilder().setCycleStartDay(0).build()

        assertThat(mapper.toDomain(proto).cycleStartDay)
            .isEqualTo(PeriodPreferences.DEFAULT_CYCLE_START_DAY)
    }

    @Test
    fun `day 25 survives proto round-trip`() {
        val preferences = PeriodPreferences(cycleStartDay = 25)

        val restored = mapper.toDomain(mapper.toProto(preferences))

        assertThat(restored).isEqualTo(preferences)
    }
}
