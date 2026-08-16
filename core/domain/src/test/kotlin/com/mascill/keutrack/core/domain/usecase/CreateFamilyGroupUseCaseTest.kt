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

class CreateFamilyGroupUseCaseTest {

    private val familyRepo = FakeFamilyRepository()
    private val userRepo = FakeUserRepository()
    private val walletRepo = FakeWalletRepository()
    private val syncRepo = FakeSyncRepository()
    private val useCase = CreateFamilyGroupUseCase(familyRepo, userRepo, walletRepo, syncRepo)

    @Test
    fun `name shorter than 2 chars returns failure`() = runTest {
        val result = useCase("A")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("minimal")
        assertThat(familyRepo.createCalls).isEmpty()
    }

    @Test
    fun `name longer than 40 chars returns failure`() = runTest {
        val result = useCase("A".repeat(41))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("maksimal")
        assertThat(familyRepo.createCalls).isEmpty()
    }

    @Test
    fun `user not logged in returns failure`() = runTest {
        userRepo.currentUser.value = null

        val result = useCase("Keluarga Irul")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("belum masuk")
    }

    @Test
    fun `user already in family returns failure`() = runTest {
        userRepo.currentUser.value = signedInUser(familyId = "fam-existing")

        val result = useCase("Keluarga Irul")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("sudah bergabung")
        assertThat(familyRepo.createCalls).isEmpty()
    }

    @Test
    fun `success creates family, updates membership, ensures wallet`() = runTest {
        userRepo.currentUser.value = signedInUser()
        familyRepo.createResult = Result.success(familyGroup())

        val result = useCase("Keluarga Irul")

        assertThat(result.exceptionOrNull()).isNull()
        assertThat(result.getOrNull()).isEqualTo(familyGroup())
        assertThat(familyRepo.createCalls).containsExactly("Keluarga Irul" to USER_ID)
        assertThat(userRepo.lastMembership).isEqualTo("fam-1" to FamilyRole.OWNER.value)
        assertThat(walletRepo.created).hasSize(1)
        assertThat(walletRepo.created.first().familyId).isEqualTo("fam-1")
        assertThat(walletRepo.created.first().type).isEqualTo(WalletType.FAMILY)
        assertThat(syncRepo.syncPendingWalletCalls).isEqualTo(1)
    }

    @Test
    fun `existing family wallet is not duplicated`() = runTest {
        userRepo.currentUser.value = signedInUser()
        familyRepo.createResult = Result.success(familyGroup())
        walletRepo.familyWallets = listOf(
            Wallet(
                id = "w-fam",
                ownerId = USER_ID,
                familyId = "fam-1",
                name = "Dompet existing",
                type = WalletType.FAMILY,
                balance = 10_000L,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        val result = useCase("Keluarga Irul")

        assertThat(result.exceptionOrNull()).isNull()
        assertThat(walletRepo.created).isEmpty()
    }

    @Test
    fun `sync failure falls back to enqueue`() = runTest {
        userRepo.currentUser.value = signedInUser()
        familyRepo.createResult = Result.success(familyGroup())
        syncRepo.syncPendingWalletsError = IllegalStateException("offline")

        val result = useCase("Keluarga Irul")

        assertThat(result.exceptionOrNull()).isNull()
        assertThat(syncRepo.enqueueForceCalls).isEqualTo(1)
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
        ownerId = USER_ID,
        memberIds = listOf(USER_ID),
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
    )

    private companion object {
        const val USER_ID = "user-1"
    }
}
