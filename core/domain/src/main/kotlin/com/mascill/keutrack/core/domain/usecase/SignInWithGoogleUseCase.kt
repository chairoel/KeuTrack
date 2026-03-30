package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.AuthResult
import com.mascill.keutrack.core.domain.model.TokenResult
import com.mascill.keutrack.core.domain.repository.GoogleAuthRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val googleAuthRepository: GoogleAuthRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(): AuthResult {
        return when (val tokenResult = googleAuthRepository.getGoogleIdToken()) {
            is TokenResult.Success -> userRepository.signInWithGoogle(tokenResult.idToken)
            is TokenResult.Cancelled -> AuthResult.Cancelled
            is TokenResult.Error.Network -> AuthResult.Error.Network
            is TokenResult.Error.Unknown -> AuthResult.Error.Unknown(tokenResult.message)
        }
    }
}
