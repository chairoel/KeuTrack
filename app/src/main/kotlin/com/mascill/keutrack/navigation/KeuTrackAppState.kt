package com.mascill.keutrack.navigation

import androidx.annotation.Keep
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.mascill.keutrack.core.designsystem.component.snackbar.KeuTrackSnackbarHostState
import com.mascill.keutrack.core.designsystem.component.snackbar.model.KeuTrackSnackbarDuration
import com.mascill.keutrack.core.designsystem.component.snackbar.model.KeuTrackSnackbarResult
import com.mascill.keutrack.core.designsystem.model.KeuTrackSnackbarData
import com.mascill.keutrack.core.designsystem.model.KeuTrackSnackbarTone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * KeuTrack App state
 */
@Composable
fun rememberKeuTrackAppState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    snackbarHostState: KeuTrackSnackbarHostState = remember { KeuTrackSnackbarHostState() },
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
            scaffoldState = scaffoldState,
            snackbarHostState = snackbarHostState,
        )
    }
}

@Stable
class KeuTrackAppState(
    val scaffoldState: ScaffoldState,
    val snackbarHostState: KeuTrackSnackbarHostState,
    val navController: NavHostController,
    private val coroutineScope: CoroutineScope,
) {

    private var snackBarJob: Job? = null
    private var pendingDeepLinkRoute: Any? = null

    fun stashPendingDeepLink(route: Any) {
        pendingDeepLinkRoute = route
    }

    fun consumePendingDeepLink(): Any? {
        val route = pendingDeepLinkRoute
        pendingDeepLinkRoute = null
        return route
    }

    /**
     * Method that handle backstack clearance after navigating
     */
    fun navigateAndResetStack(saveState: Boolean = false, navigateTo: (NavOptions) -> Unit) {
        val navOptions = navOptions {
            // Pop up to the start destination of the graph to
            // make sure this screen opened without backstack
            popUpTo(navController.graph.findStartDestination().id) {
                this.saveState = saveState

                inclusive = true // to pop the startDestination
            }
            // Avoid multiple copies of the same destination when
            // re-selecting the same item
            launchSingleTop = true
        }
        navigateTo(navOptions)
    }

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
     * Shows a custom KeuTrack snackbar. Concurrent calls cancel the previous job.
     */
    fun showSnackbar(
        data: KeuTrackSnackbarData,
        position: Alignment = Alignment.TopCenter,
        snackBarAction: (() -> Unit)? = null,
    ) {
        coroutineScope.launch {
            snackBarJob?.cancel()
            snackBarJob = launch {
                val snackBarResult = snackbarHostState.showSnackbar(
                    data = data,
                    position = position,
                )
                when (snackBarResult) {
                    KeuTrackSnackbarResult.Dismissed -> {}
                    KeuTrackSnackbarResult.ActionPerformed ->
                        snackBarAction?.invoke()
                            ?: snackbarHostState.currentSnackbarData?.dismiss()
                }
            }
        }
    }

    /**
     * Convenience wrapper for a simple message snackbar.
     */
    fun showSnackbar(
        message: String,
        actionLabel: String? = "X",
        duration: KeuTrackSnackbarDuration = KeuTrackSnackbarDuration.Short,
        tone: KeuTrackSnackbarTone = KeuTrackSnackbarTone.Danger,
        position: Alignment = Alignment.TopCenter,
        snackBarAction: (() -> Unit)? = null,
    ) {
        showSnackbar(
            data = KeuTrackSnackbarData(
                message = message,
                actionLabel = actionLabel,
                duration = duration,
                tone = tone,
            ),
            position = position,
            snackBarAction = snackBarAction,
        )
    }

    fun hideSnackbar() {
        snackbarHostState.currentSnackbarData?.dismiss()
    }
}

/**
 * State model when send data to NavBackStackEntry
 */
@Keep
data class StateData(
    val key: String,
    val data: Any,
)
