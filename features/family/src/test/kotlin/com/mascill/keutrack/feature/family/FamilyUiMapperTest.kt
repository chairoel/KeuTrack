package com.mascill.keutrack.feature.family

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategoryType
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.feature.family.presentation.model.FamilyUiMapper
import org.junit.Test
import java.time.Instant

class FamilyUiMapperTest {

    @Test
    fun `spend segments group monthly expense by member not category`() {
        val txs =
            listOf(
                expense(userId = "u-siti", addedByName = "Siti", amount = 300_000L, categoryId = "cat_food"),
                expense(userId = "u-siti", addedByName = "Siti", amount = 200_000L, categoryId = "cat_school"),
                expense(userId = "u-budi", addedByName = "Budi", amount = 250_000L, categoryId = "cat_food"),
            )

        val segments = FamilyUiMapper.toSpendSegmentsFromTransactions(txs)

        assertThat(segments.map { it.label }).containsExactly("Siti", "Budi").inOrder()
        assertThat(segments[0].fraction).isWithin(0.001f).of(500_000f / 750_000f)
        assertThat(segments[1].fraction).isWithin(0.001f).of(250_000f / 750_000f)
        assertThat(segments.none { it.label == "Makanan" || it.label == "Pendidikan" }).isTrue()
    }

    @Test
    fun `spend segments fall back to stored member name when addedByName is blank`() {
        val txs =
            listOf(
                expense(userId = "u-siti", addedByName = "", amount = 100_000L),
            )

        val segments =
            FamilyUiMapper.toSpendSegmentsFromTransactions(
                transactions = txs,
                memberNamesByUserId = mapOf("u-siti" to "Siti"),
            )

        assertThat(segments).hasSize(1)
        assertThat(segments.first().label).isEqualTo("Siti")
    }

    @Test
    fun `budget rows merge member budgets into one row per category`() {
        val budgets =
            listOf(
                budget(id = "b-1", categoryId = "cat_food", spent = 400_000L, limit = 1_000_000L),
                budget(id = "b-2", categoryId = "cat_food", spent = 200_000L, limit = 500_000L),
                budget(id = "b-3", categoryId = "cat_school", spent = 950_000L, limit = 1_000_000L),
            )
        val categories =
            mapOf(
                "cat_food" to category("cat_food", "Household"),
                "cat_school" to category("cat_school", "Education"),
            )

        val rows = FamilyUiMapper.toBudgetRows(budgets, categories)

        assertThat(rows.map { it.title }).containsExactly("Education", "Household").inOrder()
        assertThat(rows.first { it.title == "Household" }.spentLabel).isEqualTo("Rp 600.000")
        assertThat(rows.first { it.title == "Household" }.capLabel).isEqualTo("Rp 1.500.000")
        assertThat(rows.first { it.title == "Education" }.title).isEqualTo("Education")
    }

    @Test
    fun `budget rows come from expense categories when no budgets exist`() {
        val txs =
            listOf(
                expense(userId = "u-siti", addedByName = "Siti", amount = 300_000L, categoryId = "cat_food"),
                expense(userId = "u-budi", addedByName = "Budi", amount = 200_000L, categoryId = "cat_school"),
            )
        val categories =
            mapOf(
                "cat_food" to category("cat_food", "Household"),
                "cat_school" to category("cat_school", "Education"),
            )

        val rows =
            FamilyUiMapper.toBudgetRows(
                budgets = emptyList(),
                categoriesById = categories,
                transactions = txs,
            )

        assertThat(rows.map { it.title }).containsExactly("Household", "Education").inOrder()
        assertThat(rows.first { it.title == "Household" }.spentLabel).isEqualTo("Rp 300.000")
        assertThat(rows.first { it.title == "Education" }.spentLabel).isEqualTo("Rp 200.000")
        assertThat(rows.first { it.title == "Household" }.footnote)
            .isEqualTo("60% dari pengeluaran keluarga")
        assertThat(rows.first { it.title == "Household" }.barColorHex).isEqualTo("#FF7043")
    }

    @Test
    fun `filterSharedBudgets keeps only budgets for the current family`() {
        val own =
            budget(
                id = "b-own",
                categoryId = "cat_food",
                spent = 100_000L,
                limit = 1_000_000L,
                familyId = "fam-1",
            )
        val otherFamily =
            budget(
                id = "b-other",
                categoryId = "cat_food",
                spent = 1L,
                limit = 9_999_000L,
                familyId = "fam-2",
            )
        val personal =
            budget(
                id = "b-personal",
                categoryId = "cat_food",
                spent = 50_000L,
                limit = 200_000L,
                familyId = null,
            )

        val filtered =
            FamilyUiMapper.filterSharedBudgets(
                budgets = listOf(own, otherFamily, personal),
                familyId = "fam-1",
            )

        assertThat(filtered).containsExactly(own)
    }

    @Test
    fun `filterSharedBudgets is empty when user has no family`() {
        val shared =
            budget(
                id = "b-1",
                categoryId = "cat_food",
                spent = 100_000L,
                limit = 1_000_000L,
                familyId = "fam-1",
            )

        val filtered =
            FamilyUiMapper.filterSharedBudgets(
                budgets = listOf(shared),
                familyId = null,
            )

        assertThat(filtered).isEmpty()
    }

    @Test
    fun `budget rows use transaction spent against shared budget limit`() {
        val txs =
            listOf(
                expense(userId = "u-siti", addedByName = "Siti", amount = 400_000L, categoryId = "cat_food"),
            )
        val categories = mapOf("cat_food" to category("cat_food", "Household"))

        val rows =
            FamilyUiMapper.toBudgetRows(
                budgets = listOf(budget(id = "b-1", categoryId = "cat_food", spent = 10L, limit = 1_000_000L)),
                categoriesById = categories,
                transactions = txs,
            )

        assertThat(rows).hasSize(1)
        assertThat(rows.first().spentLabel).isEqualTo("Rp 400.000")
        assertThat(rows.first().capLabel).isEqualTo("Rp 1.000.000")
        assertThat(rows.first().footnote).isEqualTo("On track — sisa Rp 600.000")
    }

    private fun expense(
        userId: String,
        addedByName: String,
        amount: Long,
        categoryId: String = "cat_food",
    ): Transaction =
        Transaction(
            id = "tx-$userId-$amount",
            walletId = "w-fam",
            userId = userId,
            familyId = "fam-1",
            type = TransactionType.EXPENSE,
            amount = amount,
            categoryId = categoryId,
            date = Instant.parse("2026-08-10T00:00:00Z"),
            addedByName = addedByName,
        )

    private fun budget(
        id: String,
        categoryId: String,
        spent: Long,
        limit: Long,
        familyId: String? = "fam-1",
    ): Budget =
        Budget(
            id = id,
            userId = "u-1",
            familyId = familyId,
            categoryId = categoryId,
            limit = limit,
            spent = spent,
            month = "2026-08",
        )

    private fun category(id: String, name: String): Category =
        Category(
            id = id,
            name = name,
            icon = "Restaurant",
            color = "#FF7043",
            type = CategoryType.EXPENSE,
        )
}
