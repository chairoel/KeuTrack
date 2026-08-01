package com.mascill.keutrack.feature.family.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mascill.keutrack.feature.family.presentation.FamilyRouting
import kotlinx.serialization.Serializable

@Serializable
object FamilyRoute

/**
 * Family screen navigation graph extension.
 * Called from the HomeShell's nested NavHost in the :app module.
 */
fun NavGraphBuilder.familyGraph(
    onAddTransaction: () -> Unit = {},
    onViewAllTransactions: () -> Unit = {},
) {
    composable<FamilyRoute> {
        FamilyRouting(
            onAddTransaction = onAddTransaction,
            onViewAllTransactions = onViewAllTransactions,
        )
    }
}
