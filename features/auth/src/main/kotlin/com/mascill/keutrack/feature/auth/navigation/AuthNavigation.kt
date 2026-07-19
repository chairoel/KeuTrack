package com.mascill.keutrack.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.mascill.keutrack.feature.auth.presentation.AuthRouting
import com.mascill.keutrack.feature.auth.presentation.RegisterRouting
import kotlinx.serialization.Serializable

@Serializable object AuthRoute

@Serializable object RegisterRoute

/**
 * Method to simplify navigate to Auth Screen implementation
 */
fun NavController.navigateToAuth(
    navOptions: NavOptions? = null
) = navigate(route = AuthRoute, navOptions = navOptions)

/**
 * Method to simplify navigate to Register Screen implementation
 */
fun NavController.navigateToRegister(
    navOptions: NavOptions? = null
) = navigate(route = RegisterRoute, navOptions = navOptions)

/**
 * Auth screen navigation graph extension to simplify navigation graph builder
 */
fun NavGraphBuilder.authGraph(
    navToHome: () -> Unit,
    navToRegister: () -> Unit,
    navToLogin: () -> Unit,
) {
    composable<AuthRoute> {
        AuthRouting(
            navigateToHome = navToHome,
            navigateToRegister = navToRegister,
        )
    }
    composable<RegisterRoute> {
        RegisterRouting(
            navigateToHome = navToHome,
            navigateToLogin = navToLogin,
        )
    }
}
