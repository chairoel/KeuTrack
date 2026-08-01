package com.mascill.keutrack.feature.settings.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
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
import com.mascill.keutrack.core.domain.model.User
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
import com.mascill.keutrack.feature.settings.presentation.model.SettingsScreenContent
import com.mascill.keutrack.feature.settings.presentation.model.SignOutState

private const val SETTINGS_TOP_BAR_ELEVATION = 4
private const val SETTINGS_TOP_BAR_PH = 20
private const val SETTINGS_TOP_BAR_PV = 4
private const val SETTINGS_CONTENT_PH = 20
private const val SETTINGS_CONTENT_PT = 8
private const val SETTINGS_CONTENT_PB_EXTRA = 24
private const val SETTINGS_BOTTOM_NAV_CLEARANCE = 72
private const val SETTINGS_LIST_SECTION_SPACING = 16
private const val SETTINGS_SIGN_OUT_PT = 24

/**
 * Home routing to handle screen that will be showing and to handle view model flow /
 * live data collection
 */
@Composable
fun SettingsRouting(
    viewModel: SettingsViewModel = hiltViewModel(),
    onSignOutSuccess: () -> Unit = {},
) {
    val signOutState by viewModel.signOutState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentFamily by viewModel.currentFamily.collectAsStateWithLifecycle()
    val membershipLoading by viewModel.membershipLoading.collectAsStateWithLifecycle()
    val membershipMessage by viewModel.membershipMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var dialogMode by remember { mutableStateOf<SettingsFamilyMembershipMode?>(null) }

    val familyCode =
        currentFamily?.inviteCode
            ?: currentUser?.familyId
            ?: "Belum bergabung"
    val inFamily = !currentUser?.familyId.isNullOrBlank()

    val content =
        remember(currentUser, familyCode, inFamily) {
            DefaultSettingsMockContent.copy(
                profile =
                    DefaultSettingsMockContent.profile.copy(
                        displayName =
                            currentUser.greetingFirstNameOrFallback(
                                DefaultSettingsMockContent.profile.displayName,
                            ),
                        email = currentUser?.email ?: DefaultSettingsMockContent.profile.email,
                        avatar = currentUser?.photoUrl,
                    ),
                familyNetworkActive = inFamily,
                familyIdCode = familyCode,
            )
        }

    LaunchedEffect(signOutState) {
        if (signOutState is SignOutState.Success) {
            onSignOutSuccess()
        }
    }

    LaunchedEffect(membershipLoading, inFamily) {
        if (dialogMode != null && !membershipLoading && inFamily) {
            dialogMode = null
        }
    }

    BoxWithMembershipSnackbar(
        membershipMessage = membershipMessage,
        onDismissMembershipMessage = viewModel::dismissMembershipMessage,
    ) {
        SettingsScreen(
            content = content,
            signOutState = signOutState,
            onSignOutClick = { viewModel.signOut() },
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
                        currentFamily?.name ?: "Keluarga aktif",
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    dialogMode = SettingsFamilyMembershipMode.Create
                }
            },
            onCurrencySelected = {},
            onSheetsSyncChange = {},
            onExportSheets = {},
        )
    }

    dialogMode?.let { mode ->
        SettingsFamilyMembershipDialog(
            mode = mode,
            isLoading = membershipLoading,
            onDismiss = {
                if (!membershipLoading) dialogMode = null
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
private fun BoxWithMembershipSnackbar(
    membershipMessage: String?,
    onDismissMembershipMessage: () -> Unit,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        content()
        membershipMessage?.let { message ->
            Snackbar(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                action = {
                    TextButton(onClick = onDismissMembershipMessage) {
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
    content: SettingsScreenContent,
    signOutState: SignOutState = SignOutState.Idle,
    onSignOutClick: () -> Unit,
    onCopyFamilyId: () -> Unit = {},
    onInviteMember: () -> Unit = {},
    onManageCircle: () -> Unit = {},
    onCurrencySelected: (String) -> Unit = {},
    onSheetsSyncChange: (Boolean) -> Unit = {},
    onExportSheets: () -> Unit = {},
) {
    val isLoading = signOutState is SignOutState.Loading
    val errorMessage = (signOutState as? SignOutState.Error)?.message
    val pageBg = KeuTrackTheme.contentColors.pageColor

    var selectedCurrency by remember(content.primaryCurrencySelected) {
        mutableStateOf(content.primaryCurrencySelected)
    }
    var sheetsEnabled by remember(content.sheetsSyncEnabled) {
        mutableStateOf(content.sheetsSyncEnabled)
    }

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
                SettingsProfileCard(profile = content.profile)
            }

            item {
                SettingsSectionHeader(
                    title = "Family Network",
                    trailing = {
                        if (content.familyNetworkActive) {
                            SettingsStatusChip(label = "ACTIVE")
                        }
                    },
                )
            }

            item {
                SettingsFamilyIdHeroCard(
                    familyIdCode = content.familyIdCode,
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
                    subtitle = "Used for global reporting",
                    options = content.primaryCurrencyOptions,
                    selectedCode = selectedCurrency,
                    onCurrencySelected = { code ->
                        selectedCurrency = code
                        onCurrencySelected(code)
                    },
                )
            }

            item {
                SettingsSectionHeader(title = "Connected Wallets")
            }

            items(
                items = content.connectedWallets,
                key = { it.title },
            ) { wallet ->
                SettingsConnectedWalletCard(wallet = wallet)
            }

            item {
                SettingsGoogleSheetsCard(
                    syncEnabled = sheetsEnabled,
                    onSyncChange = { enabled ->
                        sheetsEnabled = enabled
                        onSheetsSyncChange(enabled)
                    },
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

private fun User?.greetingFirstNameOrFallback(fallback: String): String {
    val user = this ?: return fallback
    val fromDisplay = user.displayName.trim().split(" ").firstOrNull().orEmpty()
    if (fromDisplay.isNotEmpty()) return fromDisplay
    val fromEmail = user.email.substringBefore('@').trim()
    if (fromEmail.isNotEmpty()) {
        return fromEmail.replaceFirstChar { c -> c.titlecaseChar() }
    }
    return fallback
}

@Preview(showBackground = true, name = "Settings — Light")
@Composable
private fun SettingsScreenLightPreview() {
    KeuTrackTheme(darkTheme = false) {
        SettingsScreen(
            content = DefaultSettingsMockContent,
            onSignOutClick = {},
        )
    }
}

//@Preview(
//    name = "Settings — Dark",
//    showBackground = true,
//    uiMode = UI_MODE_NIGHT_YES,
//)
//@Composable
//private fun SettingsScreenDarkPreview() {
//    KeuTrackTheme(darkTheme = true) {
//        SettingsScreen(
//            content = DefaultSettingsMockContent,
//            onSignOutClick = {},
//        )
//    }
//}
