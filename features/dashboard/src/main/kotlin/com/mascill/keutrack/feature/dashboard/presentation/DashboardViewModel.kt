package com.mascill.keutrack.feature.dashboard.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.network.model.DomainResult
import com.mascill.keutrack.feature.dashboard.domain.repository.RouteRepository
import com.mascill.keutrack.feature.dashboard.presentation.model.SignOutState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: RouteRepository,
    private val userRepository: UserRepository,
    private val dispatcher: CommonDispatcher
) : ViewModel() {

    private val _signOutState = MutableStateFlow<SignOutState>(SignOutState.Idle)
    val signOutState: StateFlow<SignOutState> = _signOutState.asStateFlow()

    init {
        fetchRoute()
    }

    fun fetchRoute() = viewModelScope.launch(dispatcher.io) {
        val result = repository.getRouteList()
        when (result) {
            is DomainResult.Success -> Log.d("TAG", "fetchRoute: result size ${result.data.size}")
            else -> Log.d("TAG", "fetchRoute: result $result")
        }
    }

    fun signOut() {
        viewModelScope.launch(dispatcher.io) {
            _signOutState.value = SignOutState.Loading
            try {
                userRepository.signOut()
                _signOutState.value = SignOutState.Success
                Log.d("TAG", "signOut: success")
            } catch (e: Exception) {
                _signOutState.value = SignOutState.Error(e.message ?: "Sign out failed")
                Log.e("TAG", "signOut: failed", e)
            }
        }
    }
}