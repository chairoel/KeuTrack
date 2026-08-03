package com.mascill.keutrack.feature.settings.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Groups
import androidx.compose.ui.graphics.vector.ImageVector

data class SettingsProfileUi(
    val avatar: String?,
    val displayName: String,
    val email: String,
)

enum class ConnectedWalletStatusKind {
    Active,
    Shared,
}

data class ConnectedWalletUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val amountLabel: String,
    val statusLabel: String,
    val statusKind: ConnectedWalletStatusKind,
    val icon: ImageVector,
    val leadingAccent: Boolean = false,
)

/**
 * Preview-only fixture. Runtime Settings must use [SettingsUIState] from the ViewModel.
 */
data class SettingsScreenContent(
    val profile: SettingsProfileUi,
    val familyNetworkActive: Boolean,
    val familyIdCode: String,
    val primaryCurrencyOptions: List<String>,
    val primaryCurrencySelected: String,
    val connectedWallets: List<ConnectedWalletUi>,
    val sheetsSyncEnabled: Boolean,
)

fun SettingsScreenContent.toPreviewUiState(): SettingsUIState =
    SettingsUIState(
        isLoading = false,
        profile = profile,
        familyNetworkActive = familyNetworkActive,
        familyIdCode = familyIdCode,
        primaryCurrencyOptions = primaryCurrencyOptions,
        primaryCurrencySelected = primaryCurrencySelected,
        connectedWallets = connectedWallets,
        sheetsSyncEnabled = sheetsSyncEnabled,
    )

val DefaultSettingsMockContent =
    SettingsScreenContent(
        profile =
            SettingsProfileUi(
                displayName = "Adhi",
                email = "adhi.k@family.com",
                avatar = "",
            ),
        familyNetworkActive = true,
        familyIdCode = "KEU-992-KRT",
        primaryCurrencyOptions = listOf("IDR", "USD", "EUR"),
        primaryCurrencySelected = "IDR",
        connectedWallets =
            listOf(
                ConnectedWalletUi(
                    id = "preview-personal",
                    title = "Main Savings",
                    subtitle = "Personal",
                    amountLabel = "Rp 12.450.000",
                    statusLabel = "Active",
                    statusKind = ConnectedWalletStatusKind.Active,
                    icon = Icons.Filled.AccountBalance,
                    leadingAccent = false,
                ),
                ConnectedWalletUi(
                    id = "preview-family",
                    title = "Emergency Fund",
                    subtitle = "Family Vault",
                    amountLabel = "Rp 45.000.000",
                    statusLabel = "Shared",
                    statusKind = ConnectedWalletStatusKind.Shared,
                    icon = Icons.Filled.Groups,
                    leadingAccent = true,
                ),
            ),
        sheetsSyncEnabled = false,
    )
