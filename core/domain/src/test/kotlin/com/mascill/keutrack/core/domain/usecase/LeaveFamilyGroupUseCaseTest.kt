package com.mascill.keutrack.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.FamilyRole
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.repository.WalletRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class LeaveFamilyGroupUseCaseTest {

    private val familyRepo = mockk<FamilyRepository>()
    private val userRepo = mockk<UserRepository>()
    private val walletRepo = mockk<WalletRepository>()
    private val useCase = LeaveFamilyGroupUseCase(familyRepo, userRepo, walletRepo)

    @Test
    fun `user not logged in returns failure`() = runTest {
        every { userRepo.getCurrentUser() } returns flowOf(null)

        val result = useCase()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("belum masuk")
    }

    @Test
    fun `user not in family returns failure`() = runTest {
        every { userRepo.getCurrentUser() } returns flowOf(signedInUser(familyId = null))

        val result = useCase()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("belum bergabung")
    }

    @Test
    fun `owner with other members is blocked`() = runTest {
        every { userRepo.getCurrentUser() } returns flowOf(
            signedInUser(familyId = FAMILY_ID, familyRole = FamilyRole.OWNER.value),
        )
        coEvery { familyRepo.getFamilyById(FAMILY_ID) } returns familyGroup(
            ownerId = USER_ID,
            memberIds = listOf(USER_ID, "member-2"),
        )

        val result = useCase()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("tidak bisa keluar")
        coVerify(exactly = 0) { familyRepo.deleteFamily(any()) }
        coVerify(exactly = 0) { familyRepo.removeMember(any(), any()) }
    }

    @Test
    fun `sole owner deletes family group`() = runTest {
        stubSuccessfulLeave(
            user = signedInUser(familyId = FAMILY_ID, familyRole = FamilyRole.OWNER.value),
            family = familyGroup(ownerId = USER_ID, memberIds = listOf(USER_ID)),
        )
        coEvery { familyRepo.deleteFamily(FAMILY_ID) } coAnswers { Result.success(Unit) }

        val result = useCase()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { familyRepo.deleteFamily(FAMILY_ID) }
        coVerify(exactly = 0) { familyRepo.removeMember(any(), any()) }
        coVerify(exactly = 1) { userRepo.updateFamilyMembership(null, null) }
    }

    @Test
    fun `member removes self from memberIds`() = runTest {
        stubSuccessfulLeave(
            user = signedInUser(familyId = FAMILY_ID, familyRole = FamilyRole.MEMBER.value),
            family = familyGroup(ownerId = "owner-1", memberIds = listOf("owner-1", USER_ID)),
        )
        coEvery { familyRepo.removeMember(FAMILY_ID, USER_ID) } coAnswers { Result.success(Unit) }

        val result = useCase()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { familyRepo.removeMember(FAMILY_ID, USER_ID) }
        coVerify(exactly = 0) { familyRepo.deleteFamily(any()) }
        coVerify(exactly = 1) { userRepo.updateFamilyMembership(null, null) }
    }

    @Test
    fun `clears local family wallets`() = runTest {
        val familyWallet = Wallet(
            id = "w-fam",
            ownerId = USER_ID,
            familyId = FAMILY_ID,
            name = "Dompet keluarga",
            type = WalletType.FAMILY,
            balance = 0L,
            createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        )
        stubSuccessfulLeave(
            user = signedInUser(familyId = FAMILY_ID, familyRole = FamilyRole.MEMBER.value),
            family = familyGroup(ownerId = "owner-1", memberIds = listOf("owner-1", USER_ID)),
            wallets = listOf(familyWallet),
        )
        coEvery { familyRepo.removeMember(FAMILY_ID, USER_ID) } coAnswers { Result.success(Unit) }
        coEvery { walletRepo.deleteWallet("w-fam") } just runs

        val result = useCase()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { walletRepo.deleteWallet("w-fam") }
    }

    @Test
    fun `clears user familyId and familyRole`() = runTest {
        stubSuccessfulLeave(
            user = signedInUser(familyId = FAMILY_ID, familyRole = FamilyRole.MEMBER.value),
            family = familyGroup(ownerId = "owner-1", memberIds = listOf("owner-1", USER_ID)),
        )
        coEvery { familyRepo.removeMember(any(), any()) } coAnswers { Result.success(Unit) }

        val result = useCase()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { userRepo.updateFamilyMembership(familyId = null, familyRole = null) }
    }

    private fun stubSuccessfulLeave(
        user: User,
        family: FamilyGroup,
        wallets: List<Wallet> = emptyList(),
    ) {
        every { userRepo.getCurrentUser() } returns flowOf(user)
        coEvery { familyRepo.getFamilyById(FAMILY_ID) } returns family
        coEvery { userRepo.updateFamilyMembership(null, null) } coAnswers { Result.success(Unit) }
        every { walletRepo.observeWalletsByType(WalletType.FAMILY) } returns flowOf(wallets)
    }

    private fun signedInUser(
        familyId: String?,
        familyRole: String? = null,
    ) = User(
        uid = USER_ID,
        displayName = "Irul",
        email = "irul@example.com",
        photoUrl = null,
        familyId = familyId,
        familyRole = familyRole,
    )

    private fun familyGroup(
        ownerId: String,
        memberIds: List<String>,
    ) = FamilyGroup(
        id = FAMILY_ID,
        name = "Keluarga Irul",
        inviteCode = "ABCD12",
        ownerId = ownerId,
        memberIds = memberIds,
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
    )

    private companion object {
        const val USER_ID = "user-1"
        const val FAMILY_ID = "fam-1"
    }
}
