package com.mascill.keutrack.feature.dashboard.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mascill.keutrack.feature.dashboard.presentation.DashboardRouting
import kotlinx.serialization.Serializable

@Serializable object DashboardRoute

/**
 * Dashboard tab navigation graph extension.
 * Called from the HomeShell's nested NavHost in the :app module.
 */
fun NavGraphBuilder.dashboardGraph(
    onSignOutSuccess: () -> Unit
) {
    composable<DashboardRoute> {
        DashboardRouting(onSignOutSuccess = onSignOutSuccess)
    }
}
