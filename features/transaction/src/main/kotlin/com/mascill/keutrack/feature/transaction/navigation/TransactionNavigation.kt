package com.mascill.keutrack.feature.transaction.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.mascill.keutrack.feature.transaction.presentation.NewEntryRouting
import com.mascill.keutrack.feature.transaction.presentation.history.TransactionHistoryRouting
import kotlinx.serialization.Serializable

@Serializable object TransactionRoute

@Serializable object TransactionHistoryRoute

fun NavController.navigateToTransaction(
    navOptions: NavOptions? = null,
) = navigate(route = TransactionRoute, navOptions = navOptions)

fun NavController.navigateToTransactionHistory(
    navOptions: NavOptions? = null,
) = navigate(route = TransactionHistoryRoute, navOptions = navOptions)

fun NavGraphBuilder.transactionGraph(
    onBack: () -> Unit,
    onAddTransaction: () -> Unit = {},
) {
    composable<TransactionRoute> {
        NewEntryRouting(onBack = onBack)
    }
    composable<TransactionHistoryRoute> {
        TransactionHistoryRouting(
            onBack = onBack,
            onAddTransaction = onAddTransaction,
        )
    }
}
