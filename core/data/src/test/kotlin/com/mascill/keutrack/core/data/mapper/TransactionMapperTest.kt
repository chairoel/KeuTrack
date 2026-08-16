package com.mascill.keutrack.core.data.mapper

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.data.db.entity.TransactionEntity
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import org.junit.Test
import java.time.Instant

class TransactionMapperTest {

    private val mapper = TransactionMapper()

    @Test
    fun `entity to domain preserves all fields`() {
        val entity = TransactionEntity(
            id = "tx-1",
            walletId = "wallet-1",
            userId = "user-1",
            familyId = "fam-1",
            type = "income",
            amount = 25_000L,
            categoryId = "cat-gaji",
            note = "bonus",
            dateEpochMs = 1_722_470_400_000L,
            addedByName = "Irul",
            syncStatus = "SYNCED",
            createdAtEpochMs = 1_722_470_400_000L,
        )

        val domain = mapper.toDomain(entity)

        assertThat(domain.id).isEqualTo("tx-1")
        assertThat(domain.walletId).isEqualTo("wallet-1")
        assertThat(domain.userId).isEqualTo("user-1")
        assertThat(domain.familyId).isEqualTo("fam-1")
        assertThat(domain.type).isEqualTo(TransactionType.INCOME)
        assertThat(domain.amount).isEqualTo(25_000L)
        assertThat(domain.categoryId).isEqualTo("cat-gaji")
        assertThat(domain.note).isEqualTo("bonus")
        assertThat(domain.addedByName).isEqualTo("Irul")
        assertThat(domain.syncStatus).isEqualTo(SyncStatus.SYNCED)
        assertThat(domain.date).isEqualTo(Instant.ofEpochMilli(1_722_470_400_000L))
    }

    @Test
    fun `domain to entity round-trip`() {
        val original = Transaction(
            id = "tx-2",
            walletId = "wallet-2",
            userId = "user-2",
            familyId = null,
            type = TransactionType.EXPENSE,
            amount = 9_000L,
            categoryId = "cat-food",
            note = null,
            date = Instant.parse("2026-08-01T00:00:00Z"),
            addedByName = "Budi",
            syncStatus = SyncStatus.PENDING,
            createdAt = Instant.parse("2026-08-01T01:00:00Z"),
        )

        val restored = mapper.toDomain(mapper.toEntity(original))

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `unknown sync status falls back to PENDING`() {
        val entity = mapper.toEntity(
            Transaction(
                id = "tx-3",
                walletId = "w",
                userId = "u",
                type = TransactionType.EXPENSE,
                amount = 1L,
                categoryId = "c",
                date = Instant.parse("2026-08-01T00:00:00Z"),
                addedByName = "A",
                createdAt = Instant.parse("2026-08-01T00:00:00Z"),
            ),
        ).copy(syncStatus = "NOT_A_STATUS")

        assertThat(mapper.toDomain(entity).syncStatus).isEqualTo(SyncStatus.PENDING)
    }
}
