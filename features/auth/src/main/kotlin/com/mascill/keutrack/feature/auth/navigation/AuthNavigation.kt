package com.mascill.keutrack.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.mascill.keutrack.feature.auth.presentation.AuthRouting
import kotlinx.serialization.Serializable

@Serializable object AuthRoute

/**
 * Method to simplify navigate to Auth Screen implementation
 */
fun NavController.navigateToAuth(
    navOptions: NavOptions? = null
) = navigate(route = AuthRoute, navOptions = navOptions)

/**
 * Auth screen navigation graph extension to simplify navigation graph builder
 */
fun NavGraphBuilder.authGraph(
    navToHome: () -> Unit
) {
    composable<AuthRoute> {
        AuthRouting(
            navigateToHome = navToHome
        )
    }
}