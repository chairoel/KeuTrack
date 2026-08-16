package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.WalletUiPreferences
import com.mascill.keutrack.core.domain.repository.WalletUiPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWalletUiPreferencesUseCase @Inject constructor(
    private val repository: WalletUiPreferencesRepository,
) {
    operator fun invoke(): Flow<WalletUiPreferences> = repository.observe()
}
