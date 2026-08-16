package com.mascill.keutrack.core.data.mapper

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.datastore.SignedInUser
import com.mascill.keutrack.core.domain.model.User
import org.junit.Test

class SignedInUserProtoMapperTest {

    private val mapper = SignedInUserProtoMapper()

    @Test
    fun `empty uid maps to null`() {
        val proto = SignedInUser.getDefaultInstance()
        assertThat(mapper.toDomainOrNull(proto)).isNull()
    }

    @Test
    fun `all User fields survive proto round-trip`() {
        val user = User(
            uid = "user-1",
            displayName = "Chairul Amri",
            email = "irul@example.com",
            photoUrl = "https://example.com/a.png",
            currency = "USD",
            familyId = "fam-1",
            familyRole = "owner",
        )

        val restored = mapper.toDomainOrNull(mapper.toProto(user))

        assertThat(restored).isEqualTo(user)
    }

    @Test
    fun `empty optional proto fields become null`() {
        val proto = SignedInUser.newBuilder()
            .setUid("user-1")
            .setDisplayName("Irul")
            .setEmail("irul@example.com")
            .build()

        val user = mapper.toDomainOrNull(proto)!!

        assertThat(user.photoUrl).isNull()
        assertThat(user.familyId).isNull()
        assertThat(user.familyRole).isNull()
        assertThat(user.currency).isEqualTo("IDR")
    }
}
