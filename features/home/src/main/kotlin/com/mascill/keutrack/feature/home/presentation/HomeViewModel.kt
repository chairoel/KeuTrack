package com.mascill.keutrack.feature.home.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.network.model.DomainResult
import com.mascill.keutrack.feature.home.domain.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: RouteRepository,
    private val dispatcher: CommonDispatcher
) : ViewModel() {

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
}