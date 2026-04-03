package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.AuthResult
import com.mascill.keutrack.core.domain.repository.UserRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(idToken: String): AuthResult {
        return userRepository.signInWithGoogle(idToken)
    }
}
