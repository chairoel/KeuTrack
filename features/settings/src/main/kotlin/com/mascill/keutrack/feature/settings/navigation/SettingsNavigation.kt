package com.mascill.keutrack.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mascill.keutrack.feature.settings.presentation.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable object SettingsRoute

/**
 * Settings screen navigation graph extension
 */
fun NavGraphBuilder.settingsGraph() {
    composable<SettingsRoute> {
        SettingsScreen()
    }
}
