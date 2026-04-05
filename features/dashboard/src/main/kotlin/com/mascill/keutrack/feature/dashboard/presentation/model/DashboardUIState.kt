package com.mascill.keutrack.feature.dashboard.presentation.model


sealed class SignOutState {
    object Idle : SignOutState()
    object Loading : SignOutState()
    object Success : SignOutState()
    data class Error(val message: String) : SignOutState()
}

/**
 * Data class for Dashboard Screen UI State
 */
data class DashboardUIState(
    val signOutState: SignOutState = SignOutState.Idle
)