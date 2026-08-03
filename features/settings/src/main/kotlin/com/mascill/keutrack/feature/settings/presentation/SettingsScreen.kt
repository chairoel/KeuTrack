package com.mascill.keutrack.feature.settings.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.component.KeuTrackTopBar
import com.mascill.keutrack.core.designsystem.model.KeuTrackButtonStyle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.settings.presentation.components.SettingsConnectedWalletCard
import com.mascill.keutrack.feature.settings.presentation.components.SettingsFamilyActionTile
import com.mascill.keutrack.feature.settings.presentation.components.SettingsFamilyIdHeroCard
import com.mascill.keutrack.feature.settings.presentation.components.SettingsGoogleSheetsCard
import com.mascill.keutrack.feature.settings.presentation.components.SettingsPrimaryCurrencyRow
import com.mascill.keutrack.feature.settings.presentation.components.SettingsProfileCard
import com.mascill.keutrack.feature.settings.presentation.components.SettingsSectionHeader
import com.mascill.keutrack.feature.settings.presentation.components.SettingsStatusChip
import com.mascill.keutrack.feature.settings.presentation.membership.SettingsFamilyMembershipDialog
import com.mascill.keutrack.feature.settings.presentation.membership.SettingsFamilyMembershipMode
import com.mascill.keutrack.feature.settings.presentation.model.DefaultSettingsMockContent
import com.mascill.keutrack.feature.settings.presentation.model.SettingsUIState
import com.mascill.keutrack.feature.settings.presentation.model.SignOutState
import com.mascill.keutrack.feature.settings.presentation.model.toPreviewUiState

private const val SETTINGS_TOP_BAR_ELEVATION = 4
private const val SETTINGS_TOP_BAR_PH = 20
private const val SETTINGS_TOP_BAR_PV = 4
private const val SETTINGS_CONTENT_PH = 20
private const val SETTINGS_CONTENT_PT = 8
private const val SETTINGS_CONTENT_PB_EXTRA = 24
private const val SETTINGS_BOTTOM_NAV_CLEARANCE = 72
private const val SETTINGS_LIST_SECTION_SPACING = 16
private const val SETTINGS_SIGN_OUT_PT = 24
private const val SETTINGS_EMPTY_WALLETS =
    "Belum ada wallet. Tambah transaksi dari Dashboard untuk membuat wallet."

/**
 * Home routing to handle screen that will be showing and to handle view model flow /
 * live data collection
 */
@Composable
fun SettingsRouting(
    viewModel: SettingsViewModel = hiltViewModel(),
    onSignOutSuccess: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var dialogMode by remember { mutableStateOf<SettingsFamilyMembershipMode?>(null) }

    val inFamily = uiState.familyNetworkActive
    val familyCode = uiState.familyIdCode
    val snackbarMessage =
        uiState.currencyError ?: uiState.membershipMessage ?: uiState.infoMessage

    LaunchedEffect(uiState.signOutState) {
        if (uiState.signOutState is SignOutState.Success) {
            onSignOutSuccess()
        }
    }

    LaunchedEffect(uiState.membershipLoading, inFamily) {
        if (dialogMode != null && !uiState.membershipLoading && inFamily) {
            dialogMode = null
        }
    }

    BoxWithSettingsSnackbar(
        snackbarMessage = snackbarMessage,
        onDismiss = viewModel::dismissSnackbar,
    ) {
        SettingsScreen(
            uiState = uiState,
            onSignOutClick = viewModel::signOut,
            onCopyFamilyId = {
                if (!inFamily) {
                    Toast.makeText(context, "Belum bergabung dengan keluarga", Toast.LENGTH_SHORT)
                        .show()
                    return@SettingsScreen
                }
                copyToClipboard(context, familyCode)
                Toast.makeText(context, "Kode keluarga disalin", Toast.LENGTH_SHORT).show()
            },
            onInviteMember = {
                if (inFamily) {
                    copyToClipboard(context, familyCode)
                    Toast.makeText(
                        context,
                        "Bagikan kode $familyCode ke anggota baru",
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    dialogMode = SettingsFamilyMembershipMode.Join
                }
            },
            onManageCircle = {
                if (inFamily) {
                    Toast.makeText(
                        context,
                        uiState.familyDisplayName ?: "Keluarga aktif",
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    dialogMode = SettingsFamilyMembershipMode.Create
                }
            },
            onCurrencySelected = viewModel::onCurrencySelected,
            onSheetsSyncChange = { viewModel.onSheetsComingSoon() },
            onExportSheets = viewModel::onSheetsComingSoon,
        )
    }

    dialogMode?.let { mode ->
        SettingsFamilyMembershipDialog(
            mode = mode,
            isLoading = uiState.membershipLoading,
            onDismiss = {
                if (!uiState.membershipLoading) dialogMode = null
            },
            onSubmit = { value ->
                when (mode) {
                    SettingsFamilyMembershipMode.Create -> viewModel.createFamily(value)
                    SettingsFamilyMembershipMode.Join -> viewModel.joinFamily(value)
                }
            },
        )
    }
}

@Composable
private fun BoxWithSettingsSnackbar(
    snackbarMessage: String?,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        snackbarMessage?.let { message ->
            Snackbar(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                action = {
                    TextButton(onClick = onDismiss) {
                        Text("Tutup")
                    }
                },
            ) {
                Text(message)
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("family_invite_code", text))
}

@Composable
fun SettingsScreen(
    uiState: SettingsUIState,
    onSignOutClick: () -> Unit,
    onCopyFamilyId: () -> Unit = {},
    onInviteMember: () -> Unit = {},
    onManageCircle: () -> Unit = {},
    onCurrencySelected: (String) -> Unit = {},
    onSheetsSyncChange: (Boolean) -> Unit = {},
    onExportSheets: () -> Unit = {},
) {
    val isLoading = uiState.signOutState is SignOutState.Loading
    val errorMessage =
        (uiState.signOutState as? SignOutState.Error)?.message ?: uiState.errorMessage
    val pageBg = KeuTrackTheme.contentColors.pageColor

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = pageBg,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = pageBg,
                elevation = SETTINGS_TOP_BAR_ELEVATION.dp,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(
                                horizontal = SETTINGS_TOP_BAR_PH.dp,
                                vertical = SETTINGS_TOP_BAR_PV.dp,
                            ),
                ) {
                    KeuTrackTopBar(title = "Settings")
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding =
                PaddingValues(
                    start = SETTINGS_CONTENT_PH.dp,
                    end = SETTINGS_CONTENT_PH.dp,
                    top = SETTINGS_CONTENT_PT.dp,
                    bottom =
                        SETTINGS_CONTENT_PB_EXTRA.dp +
                            SETTINGS_BOTTOM_NAV_CLEARANCE.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(SETTINGS_LIST_SECTION_SPACING.dp),
        ) {
            item {
                SettingsProfileCard(profile = uiState.profile)
            }

            item {
                SettingsSectionHeader(
                    title = "Family Network",
                    trailing = {
                        if (uiState.familyNetworkActive) {
                            SettingsStatusChip(label = "ACTIVE")
                        }
                    },
                )
            }

            item {
                SettingsFamilyIdHeroCard(
                    familyIdCode = uiState.familyIdCode,
                    onCopyClick = onCopyFamilyId,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SettingsFamilyActionTile(
                        imageVector = Icons.Outlined.PersonAdd,
                        label = "Invite Member",
                        onClick = onInviteMember,
                        modifier = Modifier.weight(1f),
                    )
                    SettingsFamilyActionTile(
                        imageVector = Icons.Outlined.MoreHoriz,
                        label = "Manage Circle",
                        onClick = onManageCircle,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                SettingsPrimaryCurrencyRow(
                    title = "Primary Currency",
                    subtitle = "Saved on your profile · balances stay the same",
                    options = uiState.primaryCurrencyOptions,
                    selectedCode = uiState.primaryCurrencySelected,
                    onCurrencySelected = onCurrencySelected,
                    enabled = !uiState.isCurrencyUpdating,
                )
            }

            item {
                SettingsSectionHeader(title = "Connected Wallets")
            }

            if (uiState.connectedWallets.isEmpty()) {
                item {
                    Text(
                        text = SETTINGS_EMPTY_WALLETS,
                        style = KeuTrackTheme.typography.bodyRegular14,
                        color = KeuTrackTheme.textColors.body,
                    )
                }
            } else {
                items(
                    items = uiState.connectedWallets,
                    key = { it.id },
                ) { wallet ->
                    SettingsConnectedWalletCard(wallet = wallet)
                }
            }

            item {
                SettingsGoogleSheetsCard(
                    syncEnabled = uiState.sheetsSyncEnabled,
                    onSyncChange = onSheetsSyncChange,
                    onExportClick = onExportSheets,
                )
            }

            item {
                if (errorMessage != null) {
                    KeuTrackCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = errorMessage,
                            color = KeuTrackTheme.dangerColors.d500,
                            style = KeuTrackTheme.typography.bodyRegular14,
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(SETTINGS_SIGN_OUT_PT.dp))
                KeuTrackButton(
                    text = "Sign Out",
                    onClick = onSignOutClick,
                    enabled = !isLoading,
                    style = KeuTrackButtonStyle.Tertiary,
                    isLoading = isLoading,
                    leading = {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Settings — Light")
@Composable
private fun SettingsScreenLightPreview() {
    KeuTrackTheme(darkTheme = false) {
        SettingsScreen(
            uiState = DefaultSettingsMockContent.toPreviewUiState(),
            onSignOutClick = {},
        )
    }
}

@Preview(
    name = "Settings — Dark",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun SettingsScreenDarkPreview() {
    KeuTrackTheme(darkTheme = true) {
        SettingsScreen(
            uiState = DefaultSettingsMockContent.toPreviewUiState(),
            onSignOutClick = {},
        )
    }
}
