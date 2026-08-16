package com.mascill.keutrack.core.data.mapper

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.BudgetPeriod
import com.mascill.keutrack.core.domain.model.SyncStatus
import org.junit.Test
import java.time.Instant

class BudgetMapperTest {

    private val mapper = BudgetMapper()

    @Test
    fun `period and limit spent survive round-trip`() {
        val original = Budget(
            id = "b-1",
            userId = "user-1",
            familyId = "fam-1",
            categoryId = "cat-food",
            limit = 200_000L,
            spent = 50_000L,
            period = BudgetPeriod.MONTHLY,
            month = "2026-08",
            walletId = "w-1",
            syncStatus = SyncStatus.PENDING,
            createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        )

        val restored = mapper.toDomain(mapper.toEntity(original))

        assertThat(restored).isEqualTo(original)
        assertThat(restored.remaining).isEqualTo(150_000L)
    }
}
