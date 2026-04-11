package com.mascill.keutrack.feature.settings.presentation.model


sealed class SignOutState {
    object Idle : SignOutState()
    object Loading : SignOutState()
    object Success : SignOutState()
    data class Error(val message: String) : SignOutState()
}

/**
 * Data class for Settings Screen UI State
 */
data class SettingsUIState(
    val signOutState: SignOutState = SignOutState.Idle
)