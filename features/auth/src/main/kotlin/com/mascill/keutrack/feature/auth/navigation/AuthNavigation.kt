package com.mascill.keutrack.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.mascill.keutrack.feature.auth.presentation.LoginRouting
import com.mascill.keutrack.feature.auth.presentation.RegisterRouting
import kotlinx.serialization.Serializable

@Serializable object LoginRoute

@Serializable object RegisterRoute

/**
 * Method to simplify navigate to Login Screen implementation
 */
fun NavController.navigateToLogin(
    navOptions: NavOptions? = null
) = navigate(route = LoginRoute, navOptions = navOptions)

/**
 * Method to simplify navigate to Register Screen implementation
 */
fun NavController.navigateToRegister(
    navOptions: NavOptions? = null
) = navigate(route = RegisterRoute, navOptions = navOptions)

/**
 * Auth navigation graph extension (Login + Register)
 */
fun NavGraphBuilder.authGraph(
    navToHome: () -> Unit,
    navToRegister: () -> Unit,
    navToLogin: () -> Unit,
) {
    composable<LoginRoute> {
        LoginRouting(
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
