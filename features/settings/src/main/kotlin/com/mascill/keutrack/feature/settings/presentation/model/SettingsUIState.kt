package com.mascill.keutrack.feature.settings.presentation.model

sealed class SignOutState {
    object Idle : SignOutState()
    object Loading : SignOutState()
    object Success : SignOutState()
    data class Error(val message: String) : SignOutState()
}

/**
 * Single UI state for Settings screen (profile, family, wallets, sign-out).
 */
data class SettingsUIState(
    val isLoading: Boolean = true,
    val profile: SettingsProfileUi = SettingsProfileUi(
        avatar = null,
        displayName = "",
        email = "",
    ),
    val familyNetworkActive: Boolean = false,
    val familyIdCode: String = "",
    val familyDisplayName: String? = null,
    /** Uppercase role chip label, e.g. OWNER / MEMBER; null when not in a family. */
    val familyRoleLabel: String? = null,
    val connectedWallets: List<ConnectedWalletUi> = emptyList(),
    val sheetsSyncEnabled: Boolean = false,
    val signOutState: SignOutState = SignOutState.Idle,
    val membershipLoading: Boolean = false,
    val membershipMessage: String? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
)
