package com.mascill.keutrack.feature.family.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mascill.keutrack.feature.family.presentation.FamilyScreen
import kotlinx.serialization.Serializable

@Serializable object FamilyRoute

/**
 * Family screen navigation graph extension
 */
fun NavGraphBuilder.familyGraph() {
    composable<FamilyRoute> {
        FamilyScreen()
    }
}
