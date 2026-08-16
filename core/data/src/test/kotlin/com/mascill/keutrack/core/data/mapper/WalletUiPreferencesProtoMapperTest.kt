package com.mascill.keutrack.core.data.mapper

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.WalletUiPreferences
import org.junit.Test
import com.mascill.keutrack.core.datastore.WalletUiPreferences as WalletUiPreferencesProto

class WalletUiPreferencesProtoMapperTest {

    private val mapper = WalletUiPreferencesProtoMapper()

    @Test
    fun `default proto maps to visible balances`() {
        val domain = mapper.toDomain(WalletUiPreferencesProto.getDefaultInstance())

        assertThat(domain.isPersonalBalanceVisible).isTrue()
        assertThat(domain.isFamilyBalanceVisible).isTrue()
    }

    @Test
    fun `hidden flags survive proto round-trip`() {
        val preferences = WalletUiPreferences(
            isPersonalBalanceVisible = false,
            isFamilyBalanceVisible = true,
        )

        val restored = mapper.toDomain(mapper.toProto(preferences))

        assertThat(restored).isEqualTo(preferences)
    }
}
