package com.mascill.keutrack.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mascill.keutrack.core.designsystem.component.KeuTrackBottomNav
import com.mascill.keutrack.feature.family.navigation.familyGraph
import com.mascill.keutrack.feature.dashboard.navigation.DashboardRoute
import com.mascill.keutrack.feature.dashboard.navigation.dashboardGraph
import com.mascill.keutrack.feature.settings.navigation.settingsGraph

/**
 * Home shell — the authenticated root screen that hosts:
 * • A [Scaffold] with [KeuTrackBottomNav] as the bottom bar
 * • A nested [NavHost] that switches between Dashboard / Family / Settings tabs
 *
 * This composable lives in `:app` so it can depend on all feature modules
 * without violating the "features must not depend on each other" rule.
 */
@Composable
fun HomeShell(
    onSignOutSuccess: () -> Unit = {},
) {
    val homeNavController = rememberNavController()
    HomeShellContent(
        homeNavController = homeNavController,
        onSignOutSuccess = onSignOutSuccess,
    )
}

@Composable
private fun HomeShellContent(
    homeNavController: NavHostController,
    onSignOutSuccess: () -> Unit,
) {
    val backStackEntry by homeNavController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val selectedKey = HomeNavDestination.entries
        .firstOrNull { dest ->
            currentDestination?.hasRoute(dest.route::class) == true
        }
        ?.navItem?.key
        ?: HomeNavDestination.DASHBOARD.navItem.key

    Scaffold(
        bottomBar = {
            KeuTrackBottomNav(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                items = HomeNavDestination.items,
                selectedKey = selectedKey,
                onItemClick = { key ->
                    val destination = HomeNavDestination.entries
                        .firstOrNull { it.navItem.key == key }
                        ?: return@KeuTrackBottomNav

                    homeNavController.navigate(destination.route) {
                        popUpTo(homeNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        HomeNavHost(
            navController = homeNavController,
            modifier = Modifier.padding(innerPadding),
            onSignOutSuccess = onSignOutSuccess,
        )
    }
}

/**
 * Nested NavHost for the bottom-nav tabs.
 * Each feature module exposes a `NavGraphBuilder.xxxGraph()` extension.
 */
@Composable
private fun HomeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onSignOutSuccess: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        modifier = modifier,
    ) {
        dashboardGraph(onSignOutSuccess = onSignOutSuccess)
        familyGraph()
        settingsGraph()
    }
}
