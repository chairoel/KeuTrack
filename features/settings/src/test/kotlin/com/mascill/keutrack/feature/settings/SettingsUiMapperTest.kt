package com.mascill.keutrack.feature.settings

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.FamilyRole
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.feature.settings.presentation.model.SettingsUiMapper
import org.junit.Test
import java.time.Instant

class SettingsUiMapperTest {

    @Test
    fun `greetingFirstName extracts first word of displayName`() {
        val user = User("u", "Chairul Amri", "x@y.z", null)
        assertThat(SettingsUiMapper.greetingFirstName(user)).isEqualTo("Chairul")
    }

    @Test
    fun `greetingFirstName falls back to email local part`() {
        val user = User("u", "", "irul@example.com", null)
        assertThat(SettingsUiMapper.greetingFirstName(user)).isEqualTo("Irul")
    }

    @Test
    fun `greetingFirstName returns fallback when both empty`() {
        assertThat(SettingsUiMapper.greetingFirstName(null, fallback = "Guest")).isEqualTo("Guest")
    }

    @Test
    fun `mapConnectedWallets maps personal wallet correctly`() {
        val personal = wallet("w-p", WalletType.PERSONAL, 10_000L)
        val mapped = SettingsUiMapper.mapConnectedWallets(
            WalletSummary(personal, emptyList(), 10_000L, 0L),
        )
        assertThat(mapped).hasSize(1)
        assertThat(mapped.first().subtitle).isEqualTo("Personal")
        assertThat(mapped.first().leadingAccent).isFalse()
        assertThat(mapped.first().amountLabel).isEqualTo("Rp 10.000")
    }

    @Test
    fun `mapConnectedWallets maps family wallet with accent`() {
        val family = wallet("w-f", WalletType.FAMILY, 5_000L, familyId = "fam-1")
        val mapped = SettingsUiMapper.mapConnectedWallets(
            WalletSummary(null, listOf(family), 0L, 5_000L),
        )
        assertThat(mapped.first().subtitle).isEqualTo("Family")
        assertThat(mapped.first().leadingAccent).isTrue()
        assertThat(mapped.first().statusLabel).isEqualTo("Shared")
    }

    @Test
    fun `mapConnectedWallets handles empty summary`() {
        val mapped = SettingsUiMapper.mapConnectedWallets(
            WalletSummary(null, emptyList(), 0L, 0L),
        )
        assertThat(mapped).isEmpty()
    }

    @Test
    fun `from sets familyNetworkActive when familyId present`() {
        val user = User("u", "Irul", "a@b.c", null, familyId = "fam-1", familyRole = FamilyRole.OWNER.value)
        val family = FamilyGroup(
            id = "fam-1",
            name = "Keluarga Irul",
            inviteCode = "KEU-ABC-DEF",
            ownerId = "u",
            memberIds = listOf("u"),
            createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        )
        val state = SettingsUiMapper.from(user, family, emptySummary())
        assertThat(state.familyNetworkActive).isTrue()
        assertThat(state.familyIdCode).isEqualTo("KEU-ABC-DEF")
        assertThat(state.familyRoleLabel).isEqualTo("OWNER")
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `from sets empty family code when not in family`() {
        val user = User("u", "Irul", "a@b.c", null)
        val state = SettingsUiMapper.from(user, null, emptySummary())
        assertThat(state.familyNetworkActive).isFalse()
        assertThat(state.familyIdCode).isEqualTo("Belum bergabung")
        assertThat(state.familyRoleLabel).isNull()
    }

    private fun emptySummary() = WalletSummary(null, emptyList(), 0L, 0L)

    private fun wallet(
        id: String,
        type: WalletType,
        balance: Long,
        familyId: String? = null,
    ) = Wallet(
        id = id,
        ownerId = "u",
        familyId = familyId,
        name = id,
        type = type,
        balance = balance,
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
    )
}
