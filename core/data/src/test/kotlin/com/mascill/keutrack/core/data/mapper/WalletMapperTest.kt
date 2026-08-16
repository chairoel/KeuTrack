package com.mascill.keutrack.core.data.mapper

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import org.junit.Test
import java.time.Instant

class WalletMapperTest {

    private val mapper = WalletMapper()

    @Test
    fun `type enum and balance Long are preserved on round-trip`() {
        val original = Wallet(
            id = "w-1",
            ownerId = "user-1",
            familyId = "fam-1",
            name = "Dompet Keluarga",
            type = WalletType.FAMILY,
            balance = 1_250_000L,
            currency = "IDR",
            icon = "Groups",
            color = "#123456",
            syncStatus = SyncStatus.PENDING,
            createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        )

        val restored = mapper.toDomain(mapper.toEntity(original))

        assertThat(restored).isEqualTo(original)
        assertThat(restored.balance).isEqualTo(1_250_000L)
        assertThat(restored.type).isEqualTo(WalletType.FAMILY)
    }

    @Test
    fun `unknown type falls back to PERSONAL`() {
        val entity = mapper.toEntity(
            Wallet(
                id = "w-2",
                ownerId = "user-1",
                name = "X",
                type = WalletType.PERSONAL,
                balance = 0L,
                createdAt = Instant.parse("2026-08-01T00:00:00Z"),
            ),
        ).copy(type = "unknown")

        assertThat(mapper.toDomain(entity).type).isEqualTo(WalletType.PERSONAL)
    }
}
