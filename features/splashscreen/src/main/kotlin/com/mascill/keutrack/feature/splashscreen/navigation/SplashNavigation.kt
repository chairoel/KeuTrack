package com.mascill.keutrack.feature.splashscreen.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mascill.keutrack.feature.splashscreen.presentation.SplashRouting
import kotlinx.serialization.Serializable

@Serializable object SplashRoute

/**
 * Main screen navigation graph extension to simplify navigation graph builder
 */
fun NavGraphBuilder.splashGraph(
    navToHome: () -> Unit,
    navToAuth: () -> Unit
) {
    composable<SplashRoute> {
        SplashRouting(
            navToHome = navToHome,
            navToAuth = navToAuth
        )
    }
}