package com.mascill.keutrack.navigation

import androidx.annotation.Keep
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * KeuTrack App state
 */
@Composable
fun rememberKeuTrackAppState(
    scaffoldState: ScaffoldState = rememberScaffoldState(
        snackbarHostState = remember { SnackbarHostState() }
    ),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
): KeuTrackAppState {
    return remember(
        navController,
        coroutineScope,
    ) {
        KeuTrackAppState(
            navController = navController,
            coroutineScope = coroutineScope,
            scaffoldState = scaffoldState
        )
    }
}

@Stable
class KeuTrackAppState(
    val scaffoldState: ScaffoldState,
    val navController: NavHostController,
    private val coroutineScope: CoroutineScope,
) {

    private var snackBarJob: Job? = null

    /**
     * Method to handle back
     *
     * @param data send data to NavBackStackEntry (if needed)
     */
    fun onBackClick(data: List<StateData>? = null) {
        with(navController) {
            data?.forEach {
                previousBackStackEntry?.savedStateHandle?.set(it.key, it.data)
            }

            popBackStack()
        }
    }

    /**
     * Handle show snackBar
     */
    fun showSnackBar(
        message: String,
        actionLabel: String? = "X",
        duration: SnackbarDuration = SnackbarDuration.Short,
        snackBarAction: (() -> Unit)? = null
    ) {
        coroutineScope.launch {
            snackBarJob?.cancel()
            snackBarJob = launch {
                val snackBarResult = scaffoldState.snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = duration
                )
                when (snackBarResult) {
                    SnackbarResult.Dismissed -> {}
                    SnackbarResult.ActionPerformed ->
                        snackBarAction
                            ?: scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                }
            }
        }
    }
}

/**
 * State model when send data to NavBackStackEntry
 */
@Keep
data class StateData(
    val key: String,
    val data: Any
)