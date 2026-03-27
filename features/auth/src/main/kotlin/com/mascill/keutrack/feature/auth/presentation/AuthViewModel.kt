package com.mascill.keutrack.feature.auth.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.feature.auth.domain.client.GoogleAuthClient
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
    private val userRepository: UserRepository,
    private val dispatcher: CommonDispatcher,
    private val googleAuthClient: GoogleAuthClient,
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

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch(dispatcher.io) {
            _authState.value = AuthState.Loading
            
            val result = googleAuthClient.signIn(context)
            
            if (result.idToken != null) {
                // Pass token to Firebase via Domain Layer
                val user = userRepository.signInWithGoogle(result.idToken)
                
                if (user != null) {
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Sign in failed. User is null.")
                }
            } else if (result.error != null) {
                 _authState.value = AuthState.Error(result.error)
            } else {
                // Cancellation case
                _authState.value = AuthState.Idle
            }
        }
    }
}