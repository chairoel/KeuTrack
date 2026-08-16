package com.mascill.keutrack.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.mascill.keutrack.feature.auth.navigation.authGraph
import com.mascill.keutrack.feature.auth.navigation.navigateToLogin
import com.mascill.keutrack.feature.auth.navigation.navigateToRegister
import com.mascill.keutrack.feature.splashscreen.navigation.SplashRoute
import com.mascill.keutrack.feature.splashscreen.navigation.splashGraph
import com.mascill.keutrack.feature.transaction.navigation.TransactionHistoryRoute
import com.mascill.keutrack.feature.transaction.navigation.TransactionRoute
import com.mascill.keutrack.feature.transaction.navigation.navigateToTransaction
import com.mascill.keutrack.feature.transaction.navigation.navigateToTransactionHistory
import com.mascill.keutrack.feature.transaction.navigation.transactionGraph

/**
 * Top-level navigation graph. Navigation is organized as explained at
 * https://d.android.com/jetpack/compose/nav-adaptive
 *
 * The navigation graph defined in this file defines the different top level routes. Navigation
 * within each route is handled using state and Back Handlers.
 * @param modifier a [Modifier] for parent the text field
 * @param appState App State of the app, that used to create & config navigation
 */
@Composable
fun KeuTrackNavHost(
    appState: KeuTrackAppState,
    modifier: Modifier = Modifier,
    sessionViewModel: SessionNavigationViewModel = hiltViewModel(),
) {
    val navController = appState.navController
    val isSignedIn by sessionViewModel.isSignedIn.collectAsStateWithLifecycle()
    val currentEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(isSignedIn, currentEntry) {
        if (isSignedIn != false) return@LaunchedEffect
        val entry = currentEntry ?: return@LaunchedEffect
        val destination = entry.destination
        val isProtected = destination.hasRoute<HomeRoute>() ||
            destination.hasRoute<TransactionRoute>() ||
            destination.hasRoute<TransactionHistoryRoute>()
        if (!isProtected) return@LaunchedEffect

        when {
            destination.hasRoute<TransactionRoute>() -> {
                appState.stashPendingDeepLink(entry.toRoute<TransactionRoute>())
            }
            destination.hasRoute<TransactionHistoryRoute>() -> {
                appState.stashPendingDeepLink(TransactionHistoryRoute)
            }
        }
        appState.navigateAndResetStack { navOpt ->
            navController.navigateToLogin(navOptions = navOpt)
        }
    }

    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        modifier = modifier,
        enterTransition = { NavTransitions.enter(this) },
        exitTransition = { NavTransitions.exit(this) },
        popEnterTransition = { NavTransitions.popEnter(this) },
        popExitTransition = { NavTransitions.popExit(this) },
    ) {
        splashGraph(
            navToHome = {
                appState.navigateAndResetStack { navOpt ->
                    navController.navigateToHome(navOptions = navOpt)
                }
            },
            navToAuth = {
                appState.navigateAndResetStack { navOpt ->
                    navController.navigateToLogin(navOptions = navOpt)
                }
            }
        )

        authGraph(
            navToHome = {
                val pendingDeepLink = appState.consumePendingDeepLink()
                appState.navigateAndResetStack { navOpt ->
                    navController.navigateToHome(navOptions = navOpt)
                }
                if (pendingDeepLink != null) {
                    navController.navigate(pendingDeepLink)
                }
            },
            navToRegister = { navController.navigateToRegister() },
            navToLogin = { navController.popBackStack() },
        )

        composable<HomeRoute> {
            HomeShell(
                onSignOutSuccess = {
                    appState.navigateAndResetStack { navOpt ->
                        navController.navigateToLogin(navOptions = navOpt)
                    }
                },
                onAddTransaction = { navController.navigateToTransaction() },
                onViewAllTransactions = { navController.navigateToTransactionHistory() },
            )
        }

        transactionGraph(
            onBack = { navController.popBackStack() },
            onAddTransaction = { navController.navigateToTransaction() },
        )
    }
}
