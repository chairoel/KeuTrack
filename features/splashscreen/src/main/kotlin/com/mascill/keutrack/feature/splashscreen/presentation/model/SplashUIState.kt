package com.mascill.keutrack.feature.splashscreen.presentation.model


sealed interface NavigationState {
    data object Idle : NavigationState
    data object NavigateToHome : NavigationState
    data object NavigateToAuth : NavigationState
}

/**
 * Data class for SplashScreen UI State
 */
data class SplashUIState(
    val navigationState: NavigationState = NavigationState.Idle
)
