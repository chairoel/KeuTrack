package com.mascill.keutrack.core.data.mapper

import com.mascill.keutrack.core.datastore.SignedInUser
import com.mascill.keutrack.core.domain.model.User

class SignedInUserProtoMapper {

    fun toDomainOrNull(proto: SignedInUser): User? {
        if (proto.uid.isEmpty()) return null
        return User(
            uid = proto.uid,
            displayName = proto.displayName,
            email = proto.email,
            photoUrl = proto.photoUrl.takeIf { it.isNotEmpty() },
            currency = proto.currency.ifEmpty { "IDR" },
            familyId = proto.familyId.takeIf { it.isNotEmpty() },
            familyRole = proto.familyRole.takeIf { it.isNotEmpty() },
        )
    }

    fun toProto(user: User): SignedInUser =
        SignedInUser.newBuilder()
            .setUid(user.uid)
            .setDisplayName(user.displayName)
            .setEmail(user.email)
            .setPhotoUrl(user.photoUrl.orEmpty())
            .setCurrency(user.currency)
            .setFamilyId(user.familyId.orEmpty())
            .setFamilyRole(user.familyRole.orEmpty())
            .build()
}
