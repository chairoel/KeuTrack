package com.mascill.keutrack.feature.dashboard.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.network.model.DomainResult
import com.mascill.keutrack.feature.dashboard.domain.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: RouteRepository,
    private val dispatcher: CommonDispatcher,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { _currentUser.value = it }
        }
        fetchRoute()
    }

    fun fetchRoute() = viewModelScope.launch(dispatcher.io) {
        val result = repository.getRouteList()
        when (result) {
            is DomainResult.Success -> Log.d("TAG", "fetchRoute: result size ${result.data.size}")
            else -> Log.d("TAG", "fetchRoute: result $result")
        }
    }
}