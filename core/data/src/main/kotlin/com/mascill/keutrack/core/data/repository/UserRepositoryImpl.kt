package com.mascill.keutrack.core.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.mascill.keutrack.core.data.datasource.AuthNetworkDataSource
import com.mascill.keutrack.core.data.datasource.UserProfileLocalDataSource
import com.mascill.keutrack.core.data.mapper.AuthUserMapper
import com.mascill.keutrack.core.domain.model.AuthResult
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.CancellationException
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val authDataSource: AuthNetworkDataSource,
    private val mapper: AuthUserMapper,
    private val userProfileLocalDataSource: UserProfileLocalDataSource,
) : UserRepository {

    override fun getCurrentUser(): Flow<User?> =
        userProfileLocalDataSource.observeSignedInUser()
            .onStart {
                val user = mapper.mapToDomainOrNull(authDataSource.getCurrentUser())
                if (user != null) {
                    userProfileLocalDataSource.persist(user)
                }
            }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val dto = authDataSource.signInWithGoogle(idToken)
            val user = dto?.let { mapper.mapToDomain(it) }
            if (user != null) {
                userProfileLocalDataSource.persist(user)
                AuthResult.Success(user)
            }
            else AuthResult.Error.UserNotFound
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseNetworkException) {
            AuthResult.Error.Network
        } catch (e: FirebaseAuthException) {
            AuthResult.Error.InvalidCredential
        } catch (e: Exception) {
            AuthResult.Error.Unknown(e.message)
        }
    }

    override suspend fun signOut() {
        userProfileLocalDataSource.clear()
        authDataSource.signOut()
    }

    override suspend fun syncUserProfile() {
        // TODO: Implement user profile sync to Firestore
    }
}
