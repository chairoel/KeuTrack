package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.GoogleSignInResult
import com.mascill.keutrack.core.domain.repository.GoogleAuthRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val googleAuthRepository: GoogleAuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): GoogleSignInResult {
        val googleResult = googleAuthRepository.getGoogleIdToken()
        if (googleResult.idToken != null) {
            val user = userRepository.signInWithGoogle(googleResult.idToken)
            return if (user != null) {
                GoogleSignInResult(idToken = googleResult.idToken, error = null)
            } else {
                GoogleSignInResult(idToken = null, error = "Firebase Sign in failed. User is null.")
            }
        }
        return googleResult
    }
}
