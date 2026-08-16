package com.mascill.keutrack.feature.transaction.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.mascill.keutrack.feature.transaction.presentation.NewEntryRouting
import com.mascill.keutrack.feature.transaction.presentation.history.TransactionHistoryRouting
import kotlinx.serialization.Serializable

@Serializable
data class TransactionRoute(val transactionId: String? = null)

@Serializable
data class TransactionHistoryRoute(val familyOnly: Boolean = false)

object TransactionDeepLinks {
    const val SCHEME = "keutrack"
    const val TRANSACTION = "keutrack://transaction/{transactionId}"
    const val TRANSACTION_NEW = "keutrack://transaction"
    const val HISTORY = "keutrack://transactions"
}

fun NavController.navigateToTransaction(
    transactionId: String? = null,
    navOptions: NavOptions? = null,
) = navigate(route = TransactionRoute(transactionId = transactionId), navOptions = navOptions)

fun NavController.navigateToTransactionHistory(
    familyOnly: Boolean = false,
    navOptions: NavOptions? = null,
) = navigate(
    route = TransactionHistoryRoute(familyOnly = familyOnly),
    navOptions = navOptions,
)

fun NavGraphBuilder.transactionGraph(
    onBack: () -> Unit,
    onAddTransaction: () -> Unit = {},
) {
    composable<TransactionRoute>(
        deepLinks = listOf(
            navDeepLink { uriPattern = TransactionDeepLinks.TRANSACTION },
            navDeepLink { uriPattern = TransactionDeepLinks.TRANSACTION_NEW },
        ),
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<TransactionRoute>()
        NewEntryRouting(
            onBack = onBack,
            transactionId = route.transactionId,
        )
    }
    composable<TransactionHistoryRoute>(
        deepLinks = listOf(
            navDeepLink { uriPattern = TransactionDeepLinks.HISTORY },
        ),
    ) {
        TransactionHistoryRouting(
            onBack = onBack,
            onAddTransaction = onAddTransaction,
        )
    }
}
