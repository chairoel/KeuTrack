package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.repository.UserRepository
import javax.inject.Inject

class UpdateCurrencyUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(currency: String): Result<Unit> {
        val normalized = currency.trim().uppercase()
        if (normalized !in SUPPORTED) {
            return Result.failure(IllegalArgumentException("Currency tidak didukung"))
        }
        return userRepository.updateCurrency(normalized)
    }

    private companion object {
        val SUPPORTED = setOf("IDR", "USD", "EUR")
    }
}
