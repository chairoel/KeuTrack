package com.mascill.keutrack.feature.dashboard

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategoryType
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.feature.dashboard.presentation.model.DashboardUiMapper
import org.junit.Test
import java.time.Instant

class DashboardUiMapperTest {

    @Test
    fun `greetingFirstName extracts first word of displayName`() {
        val user = User("u", "Chairul Amri", "x@y.z", null)
        assertThat(DashboardUiMapper.greetingFirstName(user)).isEqualTo("Chairul")
    }

    @Test
    fun `greetingFirstName falls back to email local part`() {
        val user = User("u", "  ", "irul@example.com", null)
        assertThat(DashboardUiMapper.greetingFirstName(user)).isEqualTo("Irul")
    }

    @Test
    fun `greetingFirstName returns fallback when both empty`() {
        assertThat(DashboardUiMapper.greetingFirstName(null)).isEqualTo("there")
    }

    @Test
    fun `transaction maps to row UI with wallet label`() {
        val tx = Transaction(
            id = "tx-1",
            walletId = "w-fam",
            userId = "u",
            type = TransactionType.EXPENSE,
            amount = 12_500L,
            categoryId = "cat_makanan",
            note = "Nasi padang",
            date = Instant.parse("2026-08-01T00:00:00Z"),
            addedByName = "Irul",
        )
        val category = Category(
            id = "cat_makanan",
            name = "Makanan",
            icon = "Restaurant",
            color = "#FF7043",
            type = CategoryType.EXPENSE,
        )

        val rows = DashboardUiMapper.toTransactionRows(
            transactions = listOf(tx),
            categoriesById = mapOf(category.id to category),
            walletsById = mapOf("w-fam" to WalletType.FAMILY),
        )

        assertThat(rows).hasSize(1)
        assertThat(rows.first().title).isEqualTo("Nasi padang")
        assertThat(rows.first().categoryLabel).isEqualTo("Makanan")
        assertThat(rows.first().amountLabel).isEqualTo("Rp 12.500")
        assertThat(rows.first().isExpense).isTrue()
        assertThat(rows.first().walletLabel).isEqualTo("Family")
    }

    @Test
    fun `familyMemberInitials is empty without a family`() {
        val user = User("u", "Chairul Amri", "x@y.z", null)
        assertThat(DashboardUiMapper.familyMemberInitials(user, family = null)).isEmpty()
    }

    @Test
    fun `familyMemberInitials uses stored names and current user fallback`() {
        val user = User("u-1", "Chairul Amri", "x@y.z", null)
        val family = FamilyGroup(
            id = "fam-1",
            name = "Keluarga",
            inviteCode = "KEU-ABC-DEF",
            ownerId = "u-1",
            memberIds = listOf("u-1", "u-2", "u-3"),
            memberNames = mapOf("u-2" to "Budi Santoso"),
            createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        )

        assertThat(DashboardUiMapper.familyMemberInitials(user, family))
            .containsExactly("C", "B")
            .inOrder()
    }

    @Test
    fun `mapWalletTypes includes personal and family wallets`() {
        val personal = Wallet(
            id = "w-p",
            ownerId = "u",
            name = "Personal",
            type = WalletType.PERSONAL,
            balance = 1L,
            createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        )
        val family = personal.copy(id = "w-f", type = WalletType.FAMILY, name = "Family")
        val map = DashboardUiMapper.mapWalletTypes(
            WalletSummary(personal, listOf(family), 1L, 0L),
        )
        assertThat(map["w-p"]).isEqualTo(WalletType.PERSONAL)
        assertThat(map["w-f"]).isEqualTo(WalletType.FAMILY)
    }
}
