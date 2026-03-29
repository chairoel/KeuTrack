package com.mascill.keutrack.feature.home.presentation.model


sealed class SignOutState {
    object Idle : SignOutState()
    object Loading : SignOutState()
    object Success : SignOutState()
    data class Error(val message: String) : SignOutState()
}

/**
 * Data class for Home Screen UI State
 */
data class HomeUIState(
    val signOutState: SignOutState = SignOutState.Idle
)