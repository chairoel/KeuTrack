package com.mascill.keutrack.feature.transaction.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.mascill.keutrack.feature.transaction.presentation.NewEntryScreen
import kotlinx.serialization.Serializable

@Serializable object TransactionRoute

/**
 * Navigate to the new transaction entry screen
 */
fun NavController.navigateToTransaction(
    navOptions: NavOptions? = null
) = navigate(route = TransactionRoute, navOptions = navOptions)

/**
 * Transaction entry screen navigation graph extension
 */
fun NavGraphBuilder.transactionGraph(
    onBack: () -> Unit
) {
    composable<TransactionRoute> {
        NewEntryScreen(onBack = onBack)
    }
}
