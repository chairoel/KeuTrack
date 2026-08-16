package com.mascill.keutrack.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.FamilyRole
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class JoinFamilyGroupUseCaseTest {

    private val familyRepo = FakeFamilyRepository()
    private val userRepo = FakeUserRepository()
    private val walletRepo = FakeWalletRepository()
    private val syncRepo = FakeSyncRepository()
    private val syncFamilyData = SyncFamilyDataUseCase(userRepo, syncRepo)
    private val useCase = JoinFamilyGroupUseCase(familyRepo, userRepo, walletRepo, syncFamilyData)

    @Test
    fun `blank invite code returns failure`() = runTest {
        val result = useCase("   ")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("tidak boleh kosong")
        assertThat(familyRepo.lastJoin).isNull()
    }

    @Test
    fun `user already in family returns failure`() = runTest {
        userRepo.currentUser.value = signedInUser(familyId = "fam-existing")

        val result = useCase("ABCD12")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("sudah bergabung")
        assertThat(familyRepo.lastJoin).isNull()
    }

    @Test
    fun `invalid code (family not found) returns failure`() = runTest {
        userRepo.currentUser.value = signedInUser()
        familyRepo.joinResult = Result.failure(IllegalArgumentException("Kode undangan tidak valid"))

        val result = useCase("abcd12")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("tidak valid")
        assertThat(userRepo.lastMembership).isNull()
    }

    @Test
    fun `success joins family and updates membership`() = runTest {
        userRepo.currentUser.value = signedInUser()
        familyRepo.joinResult = Result.success(familyGroup())
        walletRepo.familyWallets = listOf(familyWallet())

        val result = useCase("abcd12")

        assertThat(result.exceptionOrNull()).isNull()
        assertThat(result.getOrNull()).isEqualTo(familyGroup())
        assertThat(familyRepo.lastJoin).isEqualTo("ABCD12" to USER_ID)
        assertThat(userRepo.lastMembership).isEqualTo("fam-1" to FamilyRole.MEMBER.value)
    }

    @Test
    fun `pulls canonical family wallet after join`() = runTest {
        userRepo.currentUser.value = signedInUser()
        familyRepo.joinResult = Result.success(familyGroup())
        walletRepo.familyWallets = listOf(familyWallet())

        val result = useCase("ABCD12")

        assertThat(result.exceptionOrNull()).isNull()
        assertThat(syncRepo.familySyncs).containsExactly("fam-1")
        assertThat(walletRepo.created).isEmpty()
    }

    private fun signedInUser(familyId: String? = null) = User(
        uid = USER_ID,
        displayName = "Irul",
        email = "irul@example.com",
        photoUrl = null,
        familyId = familyId,
    )

    private fun familyGroup() = FamilyGroup(
        id = "fam-1",
        name = "Keluarga Irul",
        inviteCode = "ABCD12",
        ownerId = "owner-1",
        memberIds = listOf("owner-1", USER_ID),
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
    )

    private fun familyWallet() = Wallet(
        id = "w-fam",
        ownerId = "owner-1",
        familyId = "fam-1",
        name = "Dompet Keluarga Irul",
        type = WalletType.FAMILY,
        balance = 0L,
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
    )

    private companion object {
        const val USER_ID = "user-1"
    }
}
