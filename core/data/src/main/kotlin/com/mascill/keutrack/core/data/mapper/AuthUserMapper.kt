package com.mascill.keutrack.core.data.mapper

import com.google.firebase.auth.FirebaseUser
import com.mascill.keutrack.core.data.model.AuthUserResponse
import com.mascill.keutrack.core.domain.model.User

class AuthUserMapper {

    /**
     * Mapping [FirebaseUser] to [AuthUserResponse]
     */
    fun mapToResponse(user: FirebaseUser): AuthUserResponse =
        AuthUserResponse(
            uid = user.uid,
            displayName = user.displayName.orEmpty(),
            email = user.email.orEmpty(),
            photoUrl = user.photoUrl?.toString()
        )

    /**
     * Mapping [AuthUserResponse] to domain [User]
     */
    fun mapToDomain(data: AuthUserResponse?): User =
        User(
            uid = data?.uid.orEmpty(),
            displayName = data?.displayName.orEmpty(),
            email = data?.email.orEmpty(),
            photoUrl = data?.photoUrl
        )

    fun mapToDomainOrNull(data: AuthUserResponse?): User? =
        data?.let { mapToDomain(it) }
}