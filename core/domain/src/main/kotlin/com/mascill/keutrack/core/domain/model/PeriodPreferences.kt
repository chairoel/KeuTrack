package com.mascill.keutrack.core.domain.model

data class PeriodPreferences(
    val cycleStartDay: Int = DEFAULT_CYCLE_START_DAY,
) {
    companion object {
        const val DEFAULT_CYCLE_START_DAY = 1
        const val MIN_CYCLE_START_DAY = 1
        const val MAX_CYCLE_START_DAY = 28
    }
}
