package com.mascill.keutrack.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.domain.model.AuthResult
import com.mascill.keutrack.core.domain.usecase.SignInWithGoogleUseCase
import com.mascill.keutrack.feature.auth.presentation.model.AuthState
import com.mascill.keutrack.feature.auth.presentation.model.AuthUIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val dispatcher: CommonDispatcher,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    
    /**
     * UI state for the auth screen.
     */
    val authUIState: StateFlow<AuthUIState> = _authState.map { state ->
        AuthUIState(authState = state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthUIState()
    )

    fun signInWithGoogle() {
        viewModelScope.launch(dispatcher.io) {
            _authState.value = AuthState.Loading

            _authState.value = when (signInWithGoogleUseCase()) {
                is AuthResult.Success -> AuthState.Success
                is AuthResult.Cancelled -> AuthState.Idle
                is AuthResult.Error.Network -> AuthState.Error("No internet connection. Please try again.")
                is AuthResult.Error.InvalidCredential -> AuthState.Error("Invalid credential. Please try again.")
                is AuthResult.Error.UserNotFound -> AuthState.Error("Account not found. Please try again.")
                is AuthResult.Error.Unknown -> AuthState.Error("An unexpected error occurred.")
            }
        }
    }
}