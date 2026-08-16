package com.mascill.keutrack.core.data.mapper

import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.mascill.keutrack.core.data.model.AuthUserResponse
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class AuthUserMapperTest {

    private val mapper = AuthUserMapper()

    @Test
    fun `null AuthUserResponse maps to empty User`() {
        val user = mapper.mapToDomain(null)
        assertThat(user.uid).isEmpty()
        assertThat(user.displayName).isEmpty()
        assertThat(user.email).isEmpty()
        assertThat(user.photoUrl).isNull()
    }

    @Test
    fun `mapToDomainOrNull returns null for missing user`() {
        assertThat(mapper.mapToDomainOrNull(null)).isNull()
    }

    @Test
    fun `FirebaseUser with missing fields maps to empty strings`() {
        val firebaseUser = mockk<FirebaseUser>()
        every { firebaseUser.uid } returns "uid-1"
        every { firebaseUser.displayName } returns null
        every { firebaseUser.email } returns null
        every { firebaseUser.photoUrl } returns null

        val response = mapper.mapToResponse(firebaseUser)

        assertThat(response).isEqualTo(
            AuthUserResponse(
                uid = "uid-1",
                displayName = "",
                email = "",
                photoUrl = null,
            ),
        )
    }

    @Test
    fun `AuthUserResponse maps to domain User`() {
        val response = AuthUserResponse(
            uid = "uid-1",
            displayName = "Irul",
            email = "irul@example.com",
            photoUrl = "https://example.com/a.png",
        )

        val user = mapper.mapToDomain(response)

        assertThat(user.uid).isEqualTo("uid-1")
        assertThat(user.displayName).isEqualTo("Irul")
        assertThat(user.email).isEqualTo("irul@example.com")
        assertThat(user.photoUrl).isEqualTo("https://example.com/a.png")
    }
}
