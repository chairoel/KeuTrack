package com.mascill.keutrack.feature.settings.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mascill.keutrack.feature.settings.presentation.SettingsRouting
import kotlinx.serialization.Serializable

@Serializable
object SettingsRoute

/**
 * Settings screen navigation graph extension
 */
fun NavGraphBuilder.settingsGraph(
    onSignOutSuccess: () -> Unit,
) {
    composable<SettingsRoute> {
        SettingsRouting(
            onSignOutSuccess = onSignOutSuccess,
        )
    }
}
