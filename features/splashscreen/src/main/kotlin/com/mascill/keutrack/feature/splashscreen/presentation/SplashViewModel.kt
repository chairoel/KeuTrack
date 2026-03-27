package com.mascill.keutrack.feature.splashscreen.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.feature.splashscreen.presentation.model.NavigationState
import com.mascill.keutrack.feature.splashscreen.presentation.model.SplashUIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val dispatcher: CommonDispatcher
) : ViewModel() {

    /**
     * Navigation events emitted from the splash screen,
     * such as staying idle, navigating to Home, or navigating to Auth.
     */
    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Idle)

    /**
     * UI state for the splash screen.
     */
    val splashUIState: StateFlow<SplashUIState> = _navigationState.map { navState ->
        SplashUIState(navigationState = navState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SplashUIState()
    )

    /**
     * Checks pending navigation requirements when the splash screen is shown.
     */
    fun checkOnGoingNavigation() = viewModelScope.launch(dispatcher.io) {
        // Observe current user. If null -> Auth, if exists -> Home
        val user = userRepository.getCurrentUser().first()
        val nextState = if (user != null) {
            NavigationState.NavigateToHome
        } else {
            NavigationState.NavigateToAuth
        }
        _navigationState.update { nextState }
    }
}