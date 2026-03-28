package com.mascill.keutrack.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
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
            
            val result = signInWithGoogleUseCase()
            
            val error = result.error
            if (result.idToken != null) {
                _authState.value = AuthState.Success
            } else if (error != null) {
                _authState.value = AuthState.Error(error)
            } else {
                _authState.value = AuthState.Idle
            }
        }
    }
}