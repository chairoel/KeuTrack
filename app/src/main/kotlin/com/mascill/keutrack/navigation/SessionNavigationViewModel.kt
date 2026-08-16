package com.mascill.keutrack.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * App-level session observer used only for navigation redirects
 * (expired session / unauthenticated deep link).
 *
 * `null` means the first DataStore emission has not arrived yet — do not redirect.
 */
@HiltViewModel
class SessionNavigationViewModel @Inject constructor(
    userRepository: UserRepository,
) : ViewModel() {

    val isSignedIn: StateFlow<Boolean?> = userRepository.getCurrentUser()
        .map { user -> user != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}
