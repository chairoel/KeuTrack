package com.mascill.keutrack.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.mascill.keutrack.feature.home.presentation.HomeRouting
import kotlinx.serialization.Serializable

@Serializable object HomeRoute

/**
 * Method to simplify navigate to Home Screen implementation
 */
fun NavController.navigateToHome(
    navOptions: NavOptions
) = navigate(route = HomeRoute, navOptions = navOptions)

/**
 * Home screen navigation graph extension to simplify navigation graph builder
 */
fun NavGraphBuilder.homeGraph(
    navToAuth: () -> Unit
) {
    composable<HomeRoute> {
        HomeRouting(onSignOutSuccess = navToAuth)
    }
}