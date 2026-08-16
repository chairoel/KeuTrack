package com.mascill.keutrack.core.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseNetworkException
import com.mascill.keutrack.core.data.datasource.AuthNetworkDataSource
import com.mascill.keutrack.core.data.datasource.FirestoreNetworkDataSource
import com.mascill.keutrack.core.data.datasource.UserProfileLocalDataSource
import com.mascill.keutrack.core.data.mapper.AuthUserMapper
import com.mascill.keutrack.core.data.model.AuthUserResponse
import com.mascill.keutrack.core.domain.model.AuthResult
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

class UserRepositoryImplTest {

    private val authDS = mockk<AuthNetworkDataSource>(relaxed = true)
    private val firestoreDS = mockk<FirestoreNetworkDataSource>(relaxed = true)
    private val localDS = mockk<UserProfileLocalDataSource>(relaxed = true)
    private val mapper = AuthUserMapper()
    private val repo = UserRepositoryImpl(authDS, firestoreDS, mapper, localDS)

    @Test
    fun `getCurrentUser emits from local data source`() = runTest {
        val local = domainUser(currency = "IDR", familyId = "fam-1", familyRole = "owner")
        every { localDS.observeSignedInUser() } returns flowOf(local)
        every { authDS.getCurrentUser() } returns null

        repo.getCurrentUser().test {
            assertThat(awaitItem()).isEqualTo(local)
            awaitComplete()
        }
    }

    @Test
    fun `getCurrentUser refreshes identity from Auth on start`() = runTest {
        val local = domainUser(displayName = "Old", email = "old@example.com")
        val auth = authUser(displayName = "New", email = "new@example.com")
        every { localDS.observeSignedInUser() } returns flowOf(local)
        every { authDS.getCurrentUser() } returns auth
        coEvery { localDS.getSignedInUser() } returns local

        repo.getCurrentUser().test {
            awaitItem()
            awaitComplete()
        }

        coVerify {
            localDS.persist(
                match {
                    it.displayName == "New" &&
                        it.email == "new@example.com" &&
                        it.familyId == local.familyId &&
                        it.currency == local.currency
                },
            )
        }
    }

    @Test
    fun `getCurrentUser preserves local currency and membership`() = runTest {
        val local = domainUser(currency = "USD", familyId = "fam-9", familyRole = "member")
        every { localDS.observeSignedInUser() } returns flowOf(local)
        every { authDS.getCurrentUser() } returns authUser()
        coEvery { localDS.getSignedInUser() } returns local

        repo.getCurrentUser().test {
            awaitItem()
            awaitComplete()
        }

        coVerify {
            localDS.persist(
                match { it.currency == "USD" && it.familyId == "fam-9" && it.familyRole == "member" },
            )
        }
    }

    @Test
    fun `signInWithGoogle success persists user`() = runTest {
        coEvery { authDS.signInWithGoogle("token") } returns authUser()
        coEvery { firestoreDS.upsertUserProfile(any()) } just runs
        coEvery { localDS.persist(any()) } just runs

        val result = repo.signInWithGoogle("token")

        assertThat(result).isInstanceOf(AuthResult.Success::class.java)
        coVerify { firestoreDS.upsertUserProfile(match { it.uid == "user-1" }) }
        coVerify { localDS.persist(match { it.uid == "user-1" }) }
    }

    @Test
    fun `signInWithGoogle network error returns Network`() = runTest {
        coEvery { authDS.signInWithGoogle(any()) } throws FirebaseNetworkException("offline")

        val result = repo.signInWithGoogle("token")

        assertThat(result).isEqualTo(AuthResult.Error.Network)
    }

    @Test
    fun `signInWithGoogle null user returns UserNotFound`() = runTest {
        coEvery { authDS.signInWithGoogle(any()) } returns null

        val result = repo.signInWithGoogle("token")

        assertThat(result).isEqualTo(AuthResult.Error.UserNotFound)
    }

    @Test
    fun `syncUserProfile upserts then resolves from Firestore`() = runTest {
        val auth = authUser()
        val remote = domainUser(currency = "USD", familyId = "fam-1", familyRole = "owner")
        every { authDS.getCurrentUser() } returns auth
        coEvery { firestoreDS.upsertUserProfile(any()) } just runs
        coEvery { firestoreDS.getUserProfile("user-1") } returns remote
        coEvery { localDS.persist(any()) } just runs

        repo.syncUserProfile()

        coVerify { firestoreDS.upsertUserProfile(match { it.uid == "user-1" }) }
        coVerify {
            localDS.persist(
                match {
                    it.currency == "USD" &&
                        it.familyId == "fam-1" &&
                        it.displayName == auth.displayName
                },
            )
        }
    }

    @Test
    fun `syncUserProfile failure does not sign out`() = runTest {
        every { authDS.getCurrentUser() } returns authUser()
        coEvery { firestoreDS.upsertUserProfile(any()) } throws IllegalStateException("firestore down")

        repo.syncUserProfile()

        coVerify(exactly = 0) { localDS.clear() }
        coVerify(exactly = 0) { authDS.signOut() }
    }

    @Test
    fun `signOut clears local and signs out auth`() = runTest {
        repo.signOut()

        coVerify(exactly = 1) { localDS.clear() }
        coVerify(exactly = 1) { authDS.signOut() }
    }

    @Test
    fun `updateFamilyMembership writes Firestore then persists local`() = runTest {
        every { authDS.getCurrentUser() } returns authUser()
        coEvery { firestoreDS.updateFamilyMembership("user-1", "fam-1", "owner") } just runs
        coEvery { localDS.getSignedInUser() } returns domainUser()
        coEvery { localDS.persist(any()) } just runs

        val result = repo.updateFamilyMembership("fam-1", "owner")

        assertThat(result.exceptionOrNull()).isNull()
        assertThat(result.isSuccess).isTrue()
        coVerify { firestoreDS.updateFamilyMembership("user-1", "fam-1", "owner") }
        coVerify { localDS.persist(match { it.familyId == "fam-1" && it.familyRole == "owner" }) }
    }

    @Test
    fun `updateFamilyMembership failure returns Result failure`() = runTest {
        every { authDS.getCurrentUser() } returns authUser()
        coEvery {
            firestoreDS.updateFamilyMembership(any(), any(), any())
        } throws IllegalStateException("denied")

        val result = repo.updateFamilyMembership("fam-1", "member")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("denied")
    }

    private fun authUser(
        displayName: String = "Irul",
        email: String = "irul@example.com",
    ) = AuthUserResponse(
        uid = "user-1",
        displayName = displayName,
        email = email,
        photoUrl = "https://example.com/a.png",
    )

    private fun domainUser(
        displayName: String = "Irul",
        email: String = "irul@example.com",
        currency: String = "IDR",
        familyId: String? = null,
        familyRole: String? = null,
    ) = User(
        uid = "user-1",
        displayName = displayName,
        email = email,
        photoUrl = "https://example.com/a.png",
        currency = currency,
        familyId = familyId,
        familyRole = familyRole,
    )
}
