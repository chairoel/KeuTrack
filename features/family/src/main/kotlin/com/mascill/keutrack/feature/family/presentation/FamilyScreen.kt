package com.mascill.keutrack.feature.family.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.component.KeuTrackFab
import com.mascill.keutrack.core.designsystem.component.KeuTrackTopBar
import com.mascill.keutrack.core.designsystem.model.KeuTrackButtonStyle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.family.presentation.components.FamilyBreakdownCard
import com.mascill.keutrack.feature.family.presentation.components.FamilyHistoryLogSection
import com.mascill.keutrack.feature.family.presentation.components.FamilySavingTogetherCard
import com.mascill.keutrack.feature.family.presentation.components.FamilySharedBudgetsCard
import com.mascill.keutrack.feature.family.presentation.membership.FamilyMembershipDialog
import com.mascill.keutrack.feature.family.presentation.membership.FamilyMembershipDialogMode
import com.mascill.keutrack.feature.family.presentation.model.DefaultFamilyInsightsMockContent
import com.mascill.keutrack.feature.family.presentation.model.FamilyUIState
import com.mascill.keutrack.feature.family.presentation.model.toPreviewUiState

private const val FAM_FAB_LIST_CLEARANCE = 72
private const val FAM_TOP_BAR_ELEVATION = 4
private const val FAM_TOP_BAR_PH = 20
private const val FAM_TOP_BAR_PV = 4
private const val FAM_CONTENT_PH = 20
private const val FAM_CONTENT_PT = 8
private const val FAM_CONTENT_PB_EXTRA = 24
private const val FAM_LIST_SECTION_SPACING = 24
private const val FAM_HERO_WIDE_BREAKPOINT = 600
private const val FAM_BANNER_PH = 16
private const val FAM_BANNER_PV = 14
private const val FAM_BANNER_CTA_PT = 12
private const val FAM_ERROR_DISMISS = "Tutup"
private const val FAM_JOIN_BANNER =
    "Belum bergabung dengan keluarga? Buat atau gabung agar insights bersama lebih bermakna."
private const val FAM_NO_WALLET_BANNER =
    "Belum ada dompet keluarga. Setelah buat/gabung keluarga, dompet bersama akan dibuat otomatis."
private const val FAM_INVITE_BANNER_PREFIX = "Kode undangan keluarga: "

/**
 * Family tab routing — binds [FamilyViewModel] state to [FamilyScreen].
 */
@Composable
fun FamilyRouting(
    onAddTransaction: () -> Unit = {},
    onViewAllTransactions: () -> Unit = {},
    viewModel: FamilyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FamilyScreen(
        uiState = uiState,
        onViewAllHistoryClick = onViewAllTransactions,
        onAdjustTargetsClick = {},
        onFabClick = onAddTransaction,
        onCreateFamily = viewModel::createFamily,
        onJoinFamily = viewModel::joinFamily,
        onDismissMembershipMessage = viewModel::dismissMembershipMessage,
    )
}

/**
 * Family Insights — shared spending breakdown, budgets, history, and membership.
 */
@Composable
fun FamilyScreen(
    uiState: FamilyUIState,
    onViewAllHistoryClick: () -> Unit = {},
    onAdjustTargetsClick: () -> Unit = {},
    onFabClick: () -> Unit = {},
    onCreateFamily: (String) -> Unit = {},
    onJoinFamily: (String) -> Unit = {},
    onDismissError: () -> Unit = {},
    onDismissMembershipMessage: () -> Unit = {},
) {
    val pageBg = KeuTrackTheme.contentColors.pageColor
    val semantic = KeuTrackTheme.semanticColors
    var heroWidth by remember { mutableStateOf(0.dp) }
    var dialogMode by remember { mutableStateOf<FamilyMembershipDialogMode?>(null) }

    LaunchedEffect(uiState.isMembershipLoading, uiState.showJoinBanner) {
        if (dialogMode != null && !uiState.isMembershipLoading && !uiState.showJoinBanner) {
            dialogMode = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            backgroundColor = pageBg,
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = pageBg,
                    elevation = FAM_TOP_BAR_ELEVATION.dp,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = FAM_TOP_BAR_PH.dp, vertical = FAM_TOP_BAR_PV.dp),
                    ) {
                        KeuTrackTopBar(title = "Family Insights")
                    }
                }
            },
            floatingActionButton = {
                KeuTrackFab(
                    onClick = onFabClick,
                    contentDescription = "Add shared transaction",
                )
            },
        ) { innerPadding ->
            when {
                uiState.isLoading -> {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = semantic.primary)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(pageBg)
                                .padding(innerPadding),
                        contentPadding =
                            PaddingValues(
                                start = FAM_CONTENT_PH.dp,
                                end = FAM_CONTENT_PH.dp,
                                top = FAM_CONTENT_PT.dp,
                                bottom = FAM_CONTENT_PB_EXTRA.dp + FAM_FAB_LIST_CLEARANCE.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(FAM_LIST_SECTION_SPACING.dp),
                    ) {
                        if (uiState.showJoinBanner) {
                            item {
                                FamilyMembershipBanner(
                                    message = FAM_JOIN_BANNER,
                                    onCreateClick = {
                                        dialogMode = FamilyMembershipDialogMode.Create
                                    },
                                    onJoinClick = {
                                        dialogMode = FamilyMembershipDialogMode.Join
                                    },
                                )
                            }
                        } else if (!uiState.inviteCode.isNullOrBlank()) {
                            item {
                                FamilyInfoBanner(
                                    message = FAM_INVITE_BANNER_PREFIX + uiState.inviteCode,
                                )
                            }
                        }

                        if (!uiState.hasFamilyWallet) {
                            item {
                                FamilyInfoBanner(message = FAM_NO_WALLET_BANNER)
                            }
                        }

                        item {
                            FamilyScreenHeroSection(
                                uiState = uiState,
                                heroWidth = heroWidth,
                                onHeroWidthChanged = { heroWidth = it },
                            )
                        }

                        item {
                            FamilyHistoryLogSection(
                                historyRows = uiState.historyRows,
                                onViewAllClick = onViewAllHistoryClick,
                            )
                        }

                        if (uiState.showInsightCard) {
                            item {
                                FamilySavingTogetherCard(
                                    title = uiState.insightTitle,
                                    body = uiState.insightBody,
                                    ctaLabel = uiState.insightCtaLabel,
                                    onAdjustTargetsClick = onAdjustTargetsClick,
                                )
                            }
                        }
                    }
                }
            }
        }

        val snackMessage = uiState.membershipMessage ?: uiState.errorMessage
        snackMessage?.let { message ->
            Snackbar(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                action = {
                    TextButton(
                        onClick = {
                            if (uiState.membershipMessage != null) {
                                onDismissMembershipMessage()
                            } else {
                                onDismissError()
                            }
                        },
                    ) {
                        Text(FAM_ERROR_DISMISS)
                    }
                },
            ) {
                Text(message)
            }
        }
    }

    dialogMode?.let { mode ->
        FamilyMembershipDialog(
            mode = mode,
            isLoading = uiState.isMembershipLoading,
            onDismiss = {
                if (!uiState.isMembershipLoading) {
                    dialogMode = null
                }
            },
            onSubmit = { value ->
                when (mode) {
                    FamilyMembershipDialogMode.Create -> onCreateFamily(value)
                    FamilyMembershipDialogMode.Join -> onJoinFamily(value)
                }
            },
        )
    }
}

@Composable
private fun FamilyMembershipBanner(
    message: String,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val effects = KeuTrackTheme.effectTokens
    val typography = KeuTrackTheme.typography
    val shape = RoundedCornerShape(shapes.radiusMd)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .border(
                    width = effects.ghostBorderWidth,
                    color = effects.ghostBorderColor,
                    shape = shape,
                )
                .background(semantic.surfaceContainerLow)
                .padding(horizontal = FAM_BANNER_PH.dp, vertical = FAM_BANNER_PV.dp),
    ) {
        Text(
            text = message,
            style = typography.bodyRegular14,
            color = semantic.onSurfaceVariant,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = FAM_BANNER_CTA_PT.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KeuTrackButton(
                text = "Buat Keluarga",
                onClick = onCreateClick,
                style = KeuTrackButtonStyle.Primary,
                modifier = Modifier.weight(1f),
            )
            KeuTrackButton(
                text = "Gabung Kode",
                onClick = onJoinClick,
                style = KeuTrackButtonStyle.Secondary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FamilyInfoBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val effects = KeuTrackTheme.effectTokens
    val typography = KeuTrackTheme.typography
    val shape = RoundedCornerShape(shapes.radiusMd)

    Text(
        text = message,
        style = typography.bodyRegular14,
        color = semantic.onSurfaceVariant,
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .border(
                    width = effects.ghostBorderWidth,
                    color = effects.ghostBorderColor,
                    shape = shape,
                )
                .background(semantic.surfaceContainerLow)
                .padding(horizontal = FAM_BANNER_PH.dp, vertical = FAM_BANNER_PV.dp),
    )
}

@Composable
private fun FamilyScreenHeroSection(
    uiState: FamilyUIState,
    heroWidth: Dp,
    onHeroWidthChanged: (Dp) -> Unit,
) {
    val density = LocalDensity.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    onHeroWidthChanged(with(density) { coords.size.width.toDp() })
                },
    ) {
        val wide = heroWidth >= FAM_HERO_WIDE_BREAKPOINT.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FAM_LIST_SECTION_SPACING.dp),
            ) {
                FamilyBreakdownCard(
                    monthlyTotalExpense = uiState.monthlyTotalExpense,
                    spendSegments = uiState.spendSegments,
                    modifier = Modifier.weight(1f),
                )
                FamilySharedBudgetsCard(
                    budgetRows = uiState.budgetRows,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(FAM_LIST_SECTION_SPACING.dp)) {
                FamilyBreakdownCard(
                    monthlyTotalExpense = uiState.monthlyTotalExpense,
                    spendSegments = uiState.spendSegments,
                )
                FamilySharedBudgetsCard(budgetRows = uiState.budgetRows)
            }
        }
    }
}

@Preview(showBackground = true, name = "Family — Light")
@Composable
private fun FamilyScreenPreview() {
    KeuTrackTheme(darkTheme = false) {
        FamilyScreen(uiState = DefaultFamilyInsightsMockContent.toPreviewUiState())
    }
}

@Preview(
    name = "Family — Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun FamilyScreenDarkPreview() {
    KeuTrackTheme(darkTheme = true) {
        FamilyScreen(uiState = DefaultFamilyInsightsMockContent.toPreviewUiState())
    }
}
