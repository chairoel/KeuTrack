package com.mascill.keutrack.core.data.repository

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.mascill.keutrack.core.data.BuildConfig
import com.mascill.keutrack.core.data.datasource.AuthNetworkDataSource
import com.mascill.keutrack.core.data.datasource.FirestoreNetworkDataSource
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
    private val firestoreDataSource: FirestoreNetworkDataSource,
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
            val user = mapper.mapToDomainOrNull(authDataSource.signInWithGoogle(idToken))
                ?: return AuthResult.Error.UserNotFound
            completeSignIn(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mapAuthFailure(e)
        }
    }

    override suspend fun registerWithEmail(
        fullName: String,
        email: String,
        password: String,
    ): AuthResult {
        return try {
            val user = mapper.mapToDomainOrNull(
                authDataSource.registerWithEmail(email, password, fullName)
            ) ?: return AuthResult.Error.UserNotFound
            completeSignIn(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mapAuthFailure(e)
        }
    }

    override suspend fun signInWithEmail(
        email: String,
        password: String,
    ): AuthResult {
        return try {
            val authUser = mapper.mapToDomainOrNull(
                authDataSource.signInWithEmail(email, password)
            ) ?: return AuthResult.Error.UserNotFound
            try {
                val userToPersist = resolveUserForPersist(authUser)
                completeSignIn(userToPersist)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Auth succeeded; Firestore read failed before completeSignIn.
                rollbackAuthSession()
                mapFirestoreFailure(e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mapAuthFailure(e)
        }
    }

    override suspend fun signOut() {
        userProfileLocalDataSource.clear()
        authDataSource.signOut()
    }

    override suspend fun syncUserProfile() {
        // TODO: Implement user profile sync to Firestore
    }

    private suspend fun resolveUserForPersist(authUser: User): User {
        val existing = firestoreDataSource.getUserProfile(authUser.uid)
        return existing?.copy(
            displayName = authUser.displayName,
            email = authUser.email,
            photoUrl = authUser.photoUrl,
        ) ?: authUser
    }

    private suspend fun completeSignIn(user: User): AuthResult {
        return try {
            firestoreDataSource.upsertUserProfile(user)
            userProfileLocalDataSource.persist(user)
            AuthResult.Success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            rollbackAuthSession()
            mapFirestoreFailure(e)
        }
    }

    private suspend fun rollbackAuthSession() {
        userProfileLocalDataSource.clear()
        authDataSource.signOut()
    }

    private fun mapAuthFailure(e: Exception): AuthResult.Error {
        logFailure("mapAuthFailure", e)
        return when (e) {
            is FirebaseNetworkException -> AuthResult.Error.Network
            is FirebaseAuthException -> when (e.errorCode) {
                ERROR_USER_NOT_FOUND -> AuthResult.Error.UserNotFound
                ERROR_WRONG_PASSWORD,
                ERROR_INVALID_CREDENTIAL,
                ERROR_INVALID_EMAIL,
                ERROR_EMAIL_ALREADY_IN_USE,
                ERROR_WEAK_PASSWORD,
                ERROR_USER_DISABLED -> AuthResult.Error.InvalidCredential
                else -> AuthResult.Error.Unknown(e.message)
            }
            is FirebaseFirestoreException -> mapFirestoreFailure(e)
            else -> AuthResult.Error.Unknown(e.message)
        }
    }

    private fun mapFirestoreFailure(e: Exception): AuthResult.Error {
        logFailure("mapFirestoreFailure", e)
        return when (e) {
            is FirebaseNetworkException -> AuthResult.Error.Network
            is FirebaseFirestoreException -> when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AuthResult.Error.Network
                else -> AuthResult.Error.Unknown(e.message)
            }
            else -> AuthResult.Error.Unknown(e.message)
        }
    }

    private fun logFailure(tag: String, e: Exception) {
        if (BuildConfig.FLAVOR != "dev") return

        val details = buildString {
            appendLine("type=${e::class.java.name}")
            appendLine("message=${e.message}")
            when (e) {
                is FirebaseAuthException -> {
                    appendLine("authErrorCode=${e.errorCode}")
                }
                is FirebaseFirestoreException -> {
                    appendLine("firestoreCode=${e.code}")
                }
                is FirebaseNetworkException -> {
                    appendLine("network=true")
                }
            }
            var cause = e.cause
            var depth = 1
            while (cause != null) {
                appendLine("cause$depth=${cause::class.java.name}: ${cause.message}")
                if (cause is FirebaseAuthException) {
                    appendLine("cause${depth}AuthErrorCode=${cause.errorCode}")
                }
                cause = cause.cause
                depth++
            }
        }
        Log.e("UserRepository", "$tag\n$details", e)
    }

    private companion object {
        const val ERROR_USER_NOT_FOUND = "ERROR_USER_NOT_FOUND"
        const val ERROR_WRONG_PASSWORD = "ERROR_WRONG_PASSWORD"
        const val ERROR_INVALID_CREDENTIAL = "ERROR_INVALID_CREDENTIAL"
        const val ERROR_INVALID_EMAIL = "ERROR_INVALID_EMAIL"
        const val ERROR_EMAIL_ALREADY_IN_USE = "ERROR_EMAIL_ALREADY_IN_USE"
        const val ERROR_WEAK_PASSWORD = "ERROR_WEAK_PASSWORD"
        const val ERROR_USER_DISABLED = "ERROR_USER_DISABLED"
    }
}
