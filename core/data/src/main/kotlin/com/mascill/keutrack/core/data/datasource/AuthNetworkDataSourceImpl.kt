package com.mascill.keutrack.core.data.datasource

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.mascill.keutrack.core.data.mapper.AuthUserMapper
import com.mascill.keutrack.core.data.model.AuthUserResponse
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthNetworkDataSourceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val mapper: AuthUserMapper
) : AuthNetworkDataSource {
    override fun getCurrentUser(): AuthUserResponse? {
        return firebaseAuth.currentUser?.let(mapper::mapToResponse)
    }

    override suspend fun signInWithGoogle(idToken: String): AuthUserResponse? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        return authResult.user?.let(mapper::mapToResponse)
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }
}
