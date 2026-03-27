package com.mascill.keutrack.core.network.firebase

import com.google.firebase.auth.FirebaseAuth
import com.mascill.keutrack.core.network.firebase.dto.AuthUserDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthNetworkDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    fun getCurrentUser(): AuthUserDto? {
        return firebaseAuth.currentUser?.let { user ->
            AuthUserDto(
                uid = user.uid,
                displayName = user.displayName ?: "",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString()
            )
        }
    }
}
