package com.mascill.keutrack.feature.splashscreen.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val dispatcher: CommonDispatcher
) : ViewModel() {

    private val _isFetchConfig = MutableSharedFlow<Boolean>()
    val isFetchConfig = _isFetchConfig.asSharedFlow()

    init {
        fetchRoutes()
    }

    /**
     * Call dummy fetch
     */
    private fun fetchRoutes() = viewModelScope.launch(dispatcher.io) {
        delay(2000) // this to project api fetch
        _isFetchConfig.emit(false)
    }
}