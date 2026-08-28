package com.mascill.keutrack.feature.family

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.common.utils.PeriodBounds
import com.mascill.keutrack.core.common.utils.PeriodLabels
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategoryType
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.FamilyRole
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.core.designsystem.model.KeuTrackProgressTone
import com.mascill.keutrack.feature.family.presentation.model.FamilyBudgetBarTone
import com.mascill.keutrack.feature.family.presentation.model.FamilyUiMapper
import com.mascill.keutrack.feature.family.presentation.model.toProgressTone
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

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
        assertThat(rows.first { it.title == "Household" }.hasLimit).isFalse()
        assertThat(rows.first { it.title == "Household" }.tone).isEqualTo(FamilyBudgetBarTone.Neutral)
        assertThat(rows.first { it.title == "Household" }.categoryId).isEqualTo("cat_food")
        assertThat(rows.first { it.title == "Household" }.muted).isTrue()
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
        assertThat(rows.first().hasLimit).isTrue()
        assertThat(rows.first().tone).isEqualTo(FamilyBudgetBarTone.Success)
        assertThat(rows.first().categoryId).isEqualTo("cat_food")
        assertThat(rows.first().muted).isFalse()
    }

    @Test
    fun `budgetTone maps awareness thresholds`() {
        assertThat(FamilyUiMapper.budgetTone(0.60f, false)).isEqualTo(FamilyBudgetBarTone.Success)
        assertThat(FamilyUiMapper.budgetTone(0.61f, false)).isEqualTo(FamilyBudgetBarTone.Watch)
        assertThat(FamilyUiMapper.budgetTone(0.75f, false)).isEqualTo(FamilyBudgetBarTone.Watch)
        assertThat(FamilyUiMapper.budgetTone(0.76f, false)).isEqualTo(FamilyBudgetBarTone.Critical)
        assertThat(FamilyUiMapper.budgetTone(0.90f, false)).isEqualTo(FamilyBudgetBarTone.Critical)
        assertThat(FamilyUiMapper.budgetTone(0.91f, false)).isEqualTo(FamilyBudgetBarTone.Error)
        assertThat(FamilyUiMapper.budgetTone(1.10f, true)).isEqualTo(FamilyBudgetBarTone.Error)
    }

    @Test
    fun `budget bar tones map to progress bar tokens`() {
        assertThat(FamilyBudgetBarTone.Success.toProgressTone())
            .isEqualTo(KeuTrackProgressTone.Success)
        assertThat(FamilyBudgetBarTone.Watch.toProgressTone())
            .isEqualTo(KeuTrackProgressTone.Warning)
        assertThat(FamilyBudgetBarTone.Critical.toProgressTone())
            .isEqualTo(KeuTrackProgressTone.Caution)
        assertThat(FamilyBudgetBarTone.Error.toProgressTone())
            .isEqualTo(KeuTrackProgressTone.Danger)
        assertThat(FamilyBudgetBarTone.Neutral.toProgressTone())
            .isEqualTo(KeuTrackProgressTone.Primary)
    }

    @Test
    fun `budget rows at 60 percent stay success`() {
        val row = limitRow(spent = 600_000L, limit = 1_000_000L)
        assertThat(row.tone).isEqualTo(FamilyBudgetBarTone.Success)
        assertThat(row.footnote).isEqualTo("On track — sisa Rp 400.000")
        assertThat(row.hasLimit).isTrue()
    }

    @Test
    fun `budget rows at 75 percent are watch`() {
        val row = limitRow(spent = 750_000L, limit = 1_000_000L)
        assertThat(row.tone).isEqualTo(FamilyBudgetBarTone.Watch)
        assertThat(row.footnote).isEqualTo("Perhatian — sisa Rp 250.000")
    }

    @Test
    fun `budget rows at 90 percent stay critical orange`() {
        val row = limitRow(spent = 900_000L, limit = 1_000_000L)
        assertThat(row.tone).isEqualTo(FamilyBudgetBarTone.Critical)
        assertThat(row.footnote).isEqualTo("Mendekati limit (10% tersisa)")
    }

    @Test
    fun `budget rows at 91 percent are error red`() {
        val row = limitRow(spent = 910_000L, limit = 1_000_000L)
        assertThat(row.tone).isEqualTo(FamilyBudgetBarTone.Error)
        assertThat(row.footnote).isEqualTo("Limit hampir habis (9% tersisa)")
    }

    @Test
    fun `budget rows over limit stay error red`() {
        val row = limitRow(spent = 1_100_000L, limit = 1_000_000L)
        assertThat(row.tone).isEqualTo(FamilyBudgetBarTone.Error)
        assertThat(row.footnote).isEqualTo("Melebihi limit Rp 100.000")
        assertThat(row.progress).isEqualTo(1f)
    }

    @Test
    fun `two categories under 60 percent share success tone`() {
        val rows =
            FamilyUiMapper.toBudgetRows(
                budgets =
                    listOf(
                        budget(
                            id = "b-food",
                            categoryId = "cat_food",
                            spent = 300_000L,
                            limit = 1_000_000L,
                        ),
                        budget(
                            id = "b-shop",
                            categoryId = "cat_shop",
                            spent = 200_000L,
                            limit = 1_000_000L,
                        ),
                    ),
                categoriesById =
                    mapOf(
                        "cat_food" to category("cat_food", "Makanan", color = "#FF7043"),
                        "cat_shop" to category("cat_shop", "Belanja", color = "#FFA726"),
                    ),
            )

        assertThat(rows.map { it.tone }).containsExactly(
            FamilyBudgetBarTone.Success,
            FamilyBudgetBarTone.Success,
        )
        assertThat(rows.map { it.barColorHex }.toSet()).containsExactly("#FF7043", "#FFA726")
        assertThat(rows.all { it.hasLimit }).isTrue()
    }

    @Test
    fun `canEditBudgets is true only for owner with family and wallet`() {
        val owner = user(familyId = "fam-1", familyRole = FamilyRole.OWNER.value)
        val member = user(familyId = "fam-1", familyRole = FamilyRole.MEMBER.value)

        assertThat(FamilyUiMapper.canEditBudgets(owner, hasFamilyWallet = true)).isTrue()
        assertThat(FamilyUiMapper.canEditBudgets(member, hasFamilyWallet = true)).isFalse()
        assertThat(FamilyUiMapper.canEditBudgets(owner, hasFamilyWallet = false)).isFalse()
        assertThat(
            FamilyUiMapper.canEditBudgets(
                user(familyId = null, familyRole = FamilyRole.OWNER.value),
                hasFamilyWallet = true,
            ),
        ).isFalse()
        assertThat(
            FamilyUiMapper.canEditBudgets(
                owner,
                hasFamilyWallet = true,
                isCurrentCalendarMonth = false,
            ),
        ).isFalse()
    }

    @Test
    fun `toUiState sets canEditBudgets for owner with family wallet`() {
        val state =
            FamilyUiMapper.toUiState(
                user = user(familyId = "fam-1", familyRole = FamilyRole.OWNER.value),
                familyGroup = familyGroup(),
                walletSummary = WalletSummary(null, listOf(familyWallet()), 0L, 0L),
                selectedMonthTxs = emptyList(),
                priorMonthTxs = emptyList(),
                budgets = emptyList(),
                categoriesById = emptyMap(),
                selectedPeriod = PeriodBounds.containing(LocalDate.of(2026, 8, 15), 1),
                cycleStartDay = 1,
                today = LocalDate.of(2026, 8, 15),
            )

        assertThat(state.canEditBudgets).isTrue()
        assertThat(state.budgetSheet).isNull()
        assertThat(state.isBudgetSaving).isFalse()
        assertThat(state.budgetMonthLabel).isEqualTo("Agustus 2026")
        assertThat(state.selectedMonthLabel).isEqualTo("Agustus 2026")
        assertThat(state.canSelectNextMonth).isFalse()
        assertThat(state.canSelectPreviousMonth).isTrue()
    }

    @Test
    fun `toUiState uses payday range label when cycle start day is 25`() {
        val today = LocalDate.of(2026, 8, 23)
        val period = PeriodBounds.containing(today, 25)
        val state =
            FamilyUiMapper.toUiState(
                user = user(familyId = "fam-1", familyRole = FamilyRole.OWNER.value),
                familyGroup = familyGroup(),
                walletSummary = WalletSummary(null, listOf(familyWallet()), 0L, 0L),
                selectedMonthTxs = emptyList(),
                priorMonthTxs = emptyList(),
                budgets = emptyList(),
                categoriesById = emptyMap(),
                selectedPeriod = period,
                cycleStartDay = 25,
                today = today,
            )

        assertThat(state.selectedMonthLabel).isEqualTo(PeriodLabels.format(period, 25))
        assertThat(state.canSelectNextMonth).isFalse()
        assertThat(state.canEditBudgets).isTrue()
    }

    @Test
    fun `toUiState lists expense categories for the sheet picker`() {
        val state =
            FamilyUiMapper.toUiState(
                user = user(familyId = "fam-1", familyRole = FamilyRole.OWNER.value),
                familyGroup = familyGroup(),
                walletSummary = WalletSummary(null, listOf(familyWallet()), 0L, 0L),
                selectedMonthTxs = emptyList(),
                priorMonthTxs = emptyList(),
                budgets = emptyList(),
                categoriesById =
                    mapOf(
                        "cat_food" to category("cat_food", "Makanan"),
                        "cat_pay" to
                            category("cat_pay", "Gaji").copy(type = CategoryType.INCOME),
                    ),
                selectedPeriod = PeriodBounds.containing(LocalDate.of(2026, 8, 15), 1),
                cycleStartDay = 1,
                today = LocalDate.of(2026, 8, 15),
            )

        assertThat(state.expenseCategories.map { it.id }).containsExactly("cat_food")
        assertThat(state.expenseCategories.first().name).isEqualTo("Makanan")
    }

    @Test
    fun `toUiState hides canEditBudgets for member`() {
        val state =
            FamilyUiMapper.toUiState(
                user = user(familyId = "fam-1", familyRole = FamilyRole.MEMBER.value),
                familyGroup = familyGroup(),
                walletSummary = WalletSummary(null, listOf(familyWallet()), 0L, 0L),
                selectedMonthTxs = emptyList(),
                priorMonthTxs = emptyList(),
                budgets = emptyList(),
                categoriesById = emptyMap(),
                selectedPeriod = PeriodBounds.containing(LocalDate.of(2026, 8, 15), 1),
                cycleStartDay = 1,
                today = LocalDate.of(2026, 8, 15),
            )

        assertThat(state.canEditBudgets).isFalse()
    }

    @Test
    fun `toUiState selected July uses only July txs for history segments and insight vs June`() {
        val july = YearMonth.of(2026, 7)
        val august = YearMonth.of(2026, 8)
        val juneTx =
            expense(
                userId = "u-budi",
                addedByName = "Budi",
                amount = 100_000L,
                date = atMonth(YearMonth.of(2026, 6)),
            )
        val julyTx =
            expense(
                userId = "u-siti",
                addedByName = "Siti",
                amount = 200_000L,
                date = atMonth(july),
            )
        val augustTx =
            expense(
                userId = "u-budi",
                addedByName = "Budi",
                amount = 999_000L,
                date = atMonth(august),
            )

        val state =
            FamilyUiMapper.toUiState(
                user = user(familyId = "fam-1", familyRole = FamilyRole.OWNER.value),
                familyGroup = familyGroup(),
                walletSummary = WalletSummary(null, listOf(familyWallet()), 0L, 0L),
                selectedMonthTxs = listOf(julyTx, augustTx),
                priorMonthTxs = listOf(juneTx),
                budgets = emptyList(),
                categoriesById = mapOf("cat_food" to category("cat_food", "Makanan")),
                selectedPeriod = PeriodBounds.containing(LocalDate.of(2026, 7, 15), 1),
                cycleStartDay = 1,
                today = LocalDate.of(2026, 8, 15),
            )

        assertThat(state.monthlyTotalExpense).isEqualTo(200_000L)
        assertThat(state.spendSegments).hasSize(1)
        assertThat(state.spendSegments.first().label).isEqualTo("Siti")
        assertThat(state.historyRows).hasSize(1)
        assertThat(state.historyRows.first().addedByLabel).isEqualTo("Siti")
        assertThat(state.showInsightCard).isTrue()
        assertThat(state.canEditBudgets).isFalse()
        assertThat(state.canSelectNextMonth).isTrue()
        assertThat(state.selectedMonthLabel).isEqualTo("Juli 2026")
        assertThat(state.budgetMonthLabel).isEqualTo("Juli 2026")
    }

    private fun limitRow(spent: Long, limit: Long) =
        FamilyUiMapper.toBudgetRows(
            budgets =
                listOf(
                    budget(
                        id = "b-1",
                        categoryId = "cat_food",
                        spent = spent,
                        limit = limit,
                    ),
                ),
            categoriesById = mapOf("cat_food" to category("cat_food", "Household")),
        ).first()

    private fun user(
        familyId: String?,
        familyRole: String?,
    ) = User(
        uid = "user-1",
        displayName = "Irul",
        email = "a@b.c",
        photoUrl = null,
        familyId = familyId,
        familyRole = familyRole,
    )

    private fun familyGroup() =
        FamilyGroup(
            id = "fam-1",
            name = "Keluarga Irul",
            inviteCode = "KEU-ABC-DEF",
            ownerId = "user-1",
            memberIds = listOf("user-1"),
            createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        )

    private fun familyWallet() =
        Wallet(
            id = "w-fam",
            ownerId = "user-1",
            familyId = "fam-1",
            name = "Family",
            type = WalletType.FAMILY,
            balance = 0L,
        )

    private fun expense(
        userId: String,
        addedByName: String,
        amount: Long,
        categoryId: String = "cat_food",
        date: Instant = Instant.parse("2026-08-10T00:00:00Z"),
    ): Transaction =
        Transaction(
            id = "tx-$userId-$amount",
            walletId = "w-fam",
            userId = userId,
            familyId = "fam-1",
            type = TransactionType.EXPENSE,
            amount = amount,
            categoryId = categoryId,
            date = date,
            addedByName = addedByName,
        )

    private fun atMonth(yearMonth: YearMonth, day: Int = 15): Instant =
        yearMonth.atDay(day).atStartOfDay(ZoneId.systemDefault()).toInstant()

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

    private fun category(id: String, name: String, color: String = "#FF7043"): Category =
        Category(
            id = id,
            name = name,
            icon = "Restaurant",
            color = color,
            type = CategoryType.EXPENSE,
        )
}
