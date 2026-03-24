package com.mascill.keutrack.feature.splashscreen.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mascill.keutrack.feature.splashscreen.SplashRouting
import kotlinx.serialization.Serializable

@Serializable object SplashRoute

/**
 * Main screen navigation graph extension to simplify navigation graph builder
 */
fun NavGraphBuilder.splashGraph() {
    composable<SplashRoute> {
        SplashRouting()
    }
}