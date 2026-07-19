package com.mascill.keutrack.feature.auth.presentation.model

enum class AuthMethod {
    Email,
    Google,
}

sealed interface AuthState {
    data object Idle : AuthState
    data class Loading(val method: AuthMethod) : AuthState
    data class Success(val method: AuthMethod) : AuthState
    data class Error(val message: String) : AuthState
}

fun AuthState.isLoading(method: AuthMethod): Boolean =
    (this is AuthState.Loading && this.method == method) ||
        (this is AuthState.Success && this.method == method)

fun AuthState.isBusy(): Boolean =
    this is AuthState.Loading || this is AuthState.Success

/**
 * Data class for Auth Screen UI State
 */
data class AuthUIState(
    val authState: AuthState = AuthState.Idle
)
