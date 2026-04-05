package com.mascill.keutrack.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable

/**
 * Top-level route for the authenticated home shell (Scaffold + BottomNav).
 */
@Serializable object HomeRoute

/**
 * Navigate to the Home shell, typically after successful auth or splash.
 */
fun NavController.navigateToHome(
    navOptions: NavOptions
) = navigate(route = HomeRoute, navOptions = navOptions)
