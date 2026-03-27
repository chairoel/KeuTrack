package com.mascill.keutrack.core.network.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.mascill.keutrack.core.network.firebase.dto.AuthUserDto
import kotlinx.coroutines.tasks.await
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

    suspend fun signInWithGoogle(idToken: String): AuthUserDto? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        return authResult.user?.let { user ->
            AuthUserDto(
                uid = user.uid,
                displayName = user.displayName ?: "",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString()
            )
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
