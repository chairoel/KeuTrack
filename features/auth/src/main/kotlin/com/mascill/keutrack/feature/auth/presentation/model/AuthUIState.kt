package com.mascill.keutrack.feature.auth.presentation.model


sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data object Success : AuthState
    data class Error(val message: String) : AuthState
}

/**
 * Data class for Auth Screen UI State
 */
data class AuthUIState(
    val authState: AuthState = AuthState.Idle
)
