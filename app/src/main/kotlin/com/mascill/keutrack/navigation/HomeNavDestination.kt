package com.mascill.keutrack.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import com.mascill.keutrack.core.designsystem.component.KeuTrackBottomNavItem
import com.mascill.keutrack.feature.family.navigation.FamilyRoute
import com.mascill.keutrack.feature.dashboard.navigation.DashboardRoute
import com.mascill.keutrack.feature.settings.navigation.SettingsRoute

/**
 * Enum that maps each bottom-nav tab to its route and UI item (icon + label).
 *
 * Tab order: Dashboard │ Family │ Settings
 */
enum class HomeNavDestination(
    val route: Any,
    val navItem: KeuTrackBottomNavItem,
) {
    DASHBOARD(
        route = DashboardRoute,
        navItem = KeuTrackBottomNavItem(
            key = "dashboard",
            icon = Icons.Outlined.Home,
            label = "Dashboard",
        )
    ),
    FAMILY(
        route = FamilyRoute,
        navItem = KeuTrackBottomNavItem(
            key = "family",
            icon = Icons.Outlined.Person,
            label = "Family",
        )
    ),
    SETTINGS(
        route = SettingsRoute,
        navItem = KeuTrackBottomNavItem(
            key = "settings",
            icon = Icons.Outlined.Settings,
            label = "Settings",
        )
    );

    companion object {
        val items get() = entries.map { it.navItem }
    }
}
