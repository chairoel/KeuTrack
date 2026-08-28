package com.mascill.keutrack.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.repository.PeriodPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SetCycleStartDayUseCaseTest {

    private val repository = mockk<PeriodPreferencesRepository>()
    private val useCase = SetCycleStartDayUseCase(repository)

    @Test
    fun `valid day persists`() = runTest {
        coEvery { repository.setCycleStartDay(25) } just runs

        val result = useCase(25)

        assertThat(result.isSuccess).isTrue()
        coVerify { repository.setCycleStartDay(25) }
    }

    @Test
    fun `day below 1 is rejected`() = runTest {
        val result = useCase(0)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("1")
        coVerify(exactly = 0) { repository.setCycleStartDay(any()) }
    }

    @Test
    fun `day above 28 is rejected`() = runTest {
        val result = useCase(29)

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { repository.setCycleStartDay(any()) }
    }
}
