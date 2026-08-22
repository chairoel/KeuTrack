package com.mascill.keutrack.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.FamilyRole
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.BudgetRepository
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

class UpsertFamilyBudgetUseCaseTest {

    private val userRepo = FakeUserRepository()
    private val walletRepo = FakeWalletRepository()
    private val budgetRepo = mockk<BudgetRepository>()
    private val transactionRepo = mockk<TransactionRepository>()
    private val useCase =
        UpsertFamilyBudgetUseCase(userRepo, budgetRepo, walletRepo, transactionRepo)

    @Before
    fun setUp() {
        userRepo.currentUser.value = owner()
        walletRepo.familyWallets = listOf(familyWallet())
        every {
            transactionRepo.observeTransactions(
                walletId = null,
                familyId = FAMILY_ID,
                type = TransactionType.EXPENSE,
                categoryId = CATEGORY_ID,
                startDate = null,
                endDate = null,
                limit = 1_000,
            )
        } returns flowOf(emptyList())
        coEvery { budgetRepo.findFamilyBudget(any(), any(), any()) } returns null
        coEvery { budgetRepo.createBudget(any()) } just runs
        coEvery { budgetRepo.updateBudget(any()) } just runs
    }

    @Test
    fun `limit zero returns failure`() = runTest {
        val result = useCase(params(limit = 0L))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Limit harus lebih dari 0")
        coVerify(exactly = 0) { budgetRepo.createBudget(any()) }
    }

    @Test
    fun `unauthenticated user returns failure`() = runTest {
        userRepo.currentUser.value = null

        val result = useCase(params())

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("belum masuk")
        coVerify(exactly = 0) { budgetRepo.createBudget(any()) }
    }

    @Test
    fun `user not in family returns failure`() = runTest {
        userRepo.currentUser.value = owner(familyId = null)

        val result = useCase(params())

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("gabung keluarga")
        coVerify(exactly = 0) { budgetRepo.createBudget(any()) }
    }

    @Test
    fun `member is rejected`() = runTest {
        userRepo.currentUser.value = owner(familyRole = FamilyRole.MEMBER.value)

        val result = useCase(params())

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("pemilik keluarga")
        coVerify(exactly = 0) { budgetRepo.createBudget(any()) }
    }

    @Test
    fun `missing family wallet returns failure`() = runTest {
        walletRepo.familyWallets = emptyList()

        val result = useCase(params())

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Dompet keluarga belum siap")
        coVerify(exactly = 0) { budgetRepo.createBudget(any()) }
    }

    @Test
    fun `create seeds spent from family expenses in month`() = runTest {
        every {
            transactionRepo.observeTransactions(
                walletId = null,
                familyId = FAMILY_ID,
                type = TransactionType.EXPENSE,
                categoryId = CATEGORY_ID,
                startDate = null,
                endDate = null,
                limit = 1_000,
            )
        } returns flowOf(listOf(familyExpense(amount = 400_000L)))

        val result = useCase(params(limit = 1_000_000L))

        assertThat(result.isSuccess).isTrue()
        val created = result.getOrNull()
        assertThat(created?.spent).isEqualTo(400_000L)
        assertThat(created?.limit).isEqualTo(1_000_000L)
        assertThat(created?.familyId).isEqualTo(FAMILY_ID)
        assertThat(created?.walletId).isEqualTo("w-fam")
        assertThat(created?.syncStatus).isEqualTo(SyncStatus.PENDING)
        coVerify {
            budgetRepo.createBudget(match { it.spent == 400_000L && it.limit == 1_000_000L })
        }
        coVerify(exactly = 0) { budgetRepo.updateBudget(any()) }
    }

    @Test
    fun `update keeps spent and only changes limit`() = runTest {
        val existing =
            Budget(
                id = "b-existing",
                userId = USER_ID,
                familyId = FAMILY_ID,
                categoryId = CATEGORY_ID,
                limit = 1_000_000L,
                spent = 400_000L,
                month = MONTH,
                walletId = "w-fam",
                createdAt = Instant.parse("2026-08-01T00:00:00Z"),
            )
        coEvery {
            budgetRepo.findFamilyBudget(FAMILY_ID, CATEGORY_ID, MONTH)
        } returns existing

        val result = useCase(params(limit = 2_000_000L))

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.spent).isEqualTo(400_000L)
        assertThat(result.getOrNull()?.limit).isEqualTo(2_000_000L)
        assertThat(result.getOrNull()?.id).isEqualTo("b-existing")
        coVerify {
            budgetRepo.updateBudget(match { it.id == "b-existing" && it.spent == 400_000L && it.limit == 2_000_000L })
        }
        coVerify(exactly = 0) { budgetRepo.createBudget(any()) }
    }

    @Test
    fun `CancellationException is rethrown`() = runTest {
        coEvery { budgetRepo.findFamilyBudget(any(), any(), any()) } throws
            CancellationException("cancelled")

        try {
            useCase(params())
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertThat(e.message).isEqualTo("cancelled")
        }
    }

    private fun params(
        categoryId: String = CATEGORY_ID,
        limit: Long = 1_000_000L,
        month: String = MONTH,
    ) = UpsertFamilyBudgetUseCase.Params(
        categoryId = categoryId,
        limit = limit,
        month = month,
    )

    private fun owner(
        familyId: String? = FAMILY_ID,
        familyRole: String? = FamilyRole.OWNER.value,
    ) = User(
        uid = USER_ID,
        displayName = "Irul",
        email = "irul@example.com",
        photoUrl = null,
        familyId = familyId,
        familyRole = familyRole,
    )

    private fun familyWallet() =
        Wallet(
            id = "w-fam",
            ownerId = USER_ID,
            familyId = FAMILY_ID,
            name = "Keluarga",
            type = WalletType.FAMILY,
            balance = 0L,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )

    private fun familyExpense(amount: Long) =
        Transaction(
            id = "tx-$amount",
            walletId = "w-fam",
            userId = USER_ID,
            familyId = FAMILY_ID,
            type = TransactionType.EXPENSE,
            amount = amount,
            categoryId = CATEGORY_ID,
            date = Instant.parse("2026-08-15T12:00:00Z"),
            addedByName = "Irul",
        )

    private companion object {
        const val USER_ID = "user-1"
        const val FAMILY_ID = "fam-1"
        const val CATEGORY_ID = "cat-food"
        const val MONTH = "2026-08"
    }
}
