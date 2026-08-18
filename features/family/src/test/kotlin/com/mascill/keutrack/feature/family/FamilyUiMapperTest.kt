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
    ): Budget =
        Budget(
            id = id,
            userId = "u-1",
            familyId = "fam-1",
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
