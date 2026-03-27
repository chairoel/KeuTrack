package com.mascill.keutrack.core.data.repository

import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.network.firebase.AuthNetworkDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val authDataSource: AuthNetworkDataSource
) : UserRepository {

    override fun getCurrentUser(): Flow<User?> = flow {
        val dto = authDataSource.getCurrentUser()
        if (dto != null) {
            emit(
                User(
                    uid = dto.uid,
                    displayName = dto.displayName,
                    email = dto.email,
                    photoUrl = dto.photoUrl
                )
            )
        } else {
            emit(null)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): User? {
        val dto = authDataSource.signInWithGoogle(idToken)
        return dto?.let {
            User(
                uid = it.uid,
                displayName = it.displayName,
                email = it.email,
                photoUrl = it.photoUrl
            )
        }
    }

    override suspend fun signOut() {
        authDataSource.signOut()
    }

    override suspend fun syncUserProfile() {
        // TODO: Implement user profile sync to Firestore
    }
}
