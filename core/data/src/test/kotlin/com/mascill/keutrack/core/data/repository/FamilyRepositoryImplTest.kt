package com.mascill.keutrack.core.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.data.datasource.UserProfileLocalDataSource
import com.mascill.keutrack.core.data.datasource.firestore.FamilyGroupFirestoreDataSource
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.User
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

class FamilyRepositoryImplTest {

    private val remote = mockk<FamilyGroupFirestoreDataSource>(relaxed = true)
    private val localUser = mockk<UserProfileLocalDataSource>()
    private val repo = FamilyRepositoryImpl(remote, localUser)

    @Test
    fun `createFamily calls Firestore data source`() = runTest {
        coEvery { remote.createFamily(any()) } just runs

        val result = repo.createFamily("Keluarga Irul", "user-1")

        assertThat(result.exceptionOrNull()).isNull()
        assertThat(result.isSuccess).isTrue()
        val family = result.getOrNull()!!
        assertThat(family.name).isEqualTo("Keluarga Irul")
        assertThat(family.ownerId).isEqualTo("user-1")
        assertThat(family.memberIds).containsExactly("user-1")
        assertThat(family.inviteCode).startsWith("KEU-")
        coVerify { remote.createFamily(match { it.id == family.id }) }
    }

    @Test
    fun `observeCurrentFamily emits from remote when familyId present`() = runTest {
        val family = familyGroup()
        every { localUser.observeSignedInUser() } returns flowOf(signedInUser(familyId = "fam-1"))
        every { remote.observeFamily("fam-1") } returns flowOf(family)

        repo.observeCurrentFamily().test {
            assertThat(awaitItem()).isEqualTo(family)
            awaitComplete()
        }
    }

    @Test
    fun `observeCurrentFamily emits null when user has no family`() = runTest {
        every { localUser.observeSignedInUser() } returns flowOf(signedInUser(familyId = null))

        repo.observeCurrentFamily().test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }

    @Test
    fun `joinFamily fails when invite code is unknown`() = runTest {
        coEvery { remote.findByInviteCode("ABCD12") } returns null

        val result = repo.joinFamily("abcd12", "user-1")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("tidak ditemukan")
    }

    private fun signedInUser(familyId: String?) = User(
        uid = "user-1",
        displayName = "Irul",
        email = "irul@example.com",
        photoUrl = null,
        familyId = familyId,
    )

    private fun familyGroup() = FamilyGroup(
        id = "fam-1",
        name = "Keluarga Irul",
        inviteCode = "KEU-ABC-DEF",
        ownerId = "owner-1",
        memberIds = listOf("owner-1"),
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
    )
}
