package com.mascill.keutrack.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import com.mascill.keutrack.feature.auth.navigation.authGraph
import com.mascill.keutrack.feature.auth.navigation.navigateToAuth
import com.mascill.keutrack.feature.home.navigation.homeGraph
import com.mascill.keutrack.feature.home.navigation.navigateToHome
import com.mascill.keutrack.feature.splashscreen.navigation.SplashRoute
import com.mascill.keutrack.feature.splashscreen.navigation.splashGraph

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
) {
    val navController = appState.navController
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        modifier = modifier,
    ) {
        splashGraph(
            navToHome = {
                appState.navigateAndResetStack { navOpt ->
                    navController.navigateToHome(navOptions = navOpt)
                }
            },
            navToAuth = {
                appState.navigateAndResetStack { navOpt ->
                    navController.navigateToAuth(navOptions = navOpt)
                }
            }
        )

        authGraph(
            navToHome = {
                appState.navigateAndResetStack { navOpt ->
                    navController.navigateToHome(navOptions = navOpt)
                }
            }
        )

        homeGraph(
            navToAuth = {
                appState.navigateAndResetStack { navOpt ->
                    navController.navigateToAuth(navOptions = navOpt)
                }
            }
        )
    }
}