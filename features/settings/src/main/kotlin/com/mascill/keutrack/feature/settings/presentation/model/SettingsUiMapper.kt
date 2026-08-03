package com.mascill.keutrack.feature.settings.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Groups
import com.mascill.keutrack.core.designsystem.format.CurrencyFormat
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.FamilyRole
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.usecase.WalletSummary

internal object SettingsUiMapper {

    fun from(
        user: User?,
        family: FamilyGroup?,
        walletSummary: WalletSummary,
    ): SettingsUIState {
        val inFamily = !user?.familyId.isNullOrBlank()
        val familyCode =
            if (inFamily) {
                family?.inviteCode?.takeIf { it.isNotBlank() }
                    ?: EMPTY_FAMILY_CODE
            } else {
                EMPTY_FAMILY_CODE
            }

        return SettingsUIState(
            isLoading = false,
            profile =
                SettingsProfileUi(
                    avatar = user?.photoUrl,
                    displayName = greetingFirstName(user),
                    email = user?.email.orEmpty(),
                ),
            familyNetworkActive = inFamily,
            familyIdCode = familyCode,
            familyDisplayName = family?.name?.takeIf { it.isNotBlank() },
            familyRoleLabel = familyRoleLabel(user, inFamily),
            connectedWallets = mapConnectedWallets(walletSummary),
            sheetsSyncEnabled = false,
        )
    }

    private fun familyRoleLabel(user: User?, inFamily: Boolean): String? {
        if (!inFamily) return null
        val raw = user?.familyRole?.takeIf { it.isNotBlank() } ?: return null
        return FamilyRole.fromValue(raw).name
    }

    fun greetingFirstName(user: User?, fallback: String = ""): String {
        val data = user ?: return fallback
        val fromDisplay = data.displayName.trim().split(" ").firstOrNull().orEmpty()
        if (fromDisplay.isNotEmpty()) return fromDisplay
        val fromEmail = data.email.substringBefore('@').trim()
        if (fromEmail.isNotEmpty()) {
            return fromEmail.replaceFirstChar { c -> c.titlecaseChar() }
        }
        return fallback
    }

    fun mapConnectedWallets(summary: WalletSummary): List<ConnectedWalletUi> {
        val personal =
            summary.personalWallet?.let { wallet ->
                toConnectedWalletUi(wallet)
            }
        val family = summary.familyWallets.map { toConnectedWalletUi(it) }
        return buildList {
            if (personal != null) add(personal)
            addAll(family)
        }
    }

    private fun toConnectedWalletUi(wallet: Wallet): ConnectedWalletUi =
        when (wallet.type) {
            WalletType.PERSONAL ->
                ConnectedWalletUi(
                    id = wallet.id,
                    title = wallet.name,
                    subtitle = "Personal",
                    amountLabel = CurrencyFormat.formatIdr(wallet.balance),
                    statusLabel = "Active",
                    statusKind = ConnectedWalletStatusKind.Active,
                    icon = Icons.Filled.AccountBalance,
                    leadingAccent = false,
                )

            WalletType.FAMILY ->
                ConnectedWalletUi(
                    id = wallet.id,
                    title = wallet.name,
                    subtitle = "Family Vault",
                    amountLabel = CurrencyFormat.formatIdr(wallet.balance),
                    statusLabel = "Shared",
                    statusKind = ConnectedWalletStatusKind.Shared,
                    icon = Icons.Filled.Groups,
                    leadingAccent = true,
                )
        }

    private const val EMPTY_FAMILY_CODE = "Belum bergabung"
}
