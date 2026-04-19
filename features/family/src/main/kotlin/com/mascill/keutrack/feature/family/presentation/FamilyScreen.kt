package com.mascill.keutrack.feature.family.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.hilt.navigation.compose.hiltViewModel
import com.mascill.keutrack.core.designsystem.component.KeuTrackTopBar
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.family.presentation.components.FamilyBreakdownCard
import com.mascill.keutrack.feature.family.presentation.components.FamilyHistoryLogSection
import com.mascill.keutrack.feature.family.presentation.components.FamilySavingTogetherCard
import com.mascill.keutrack.feature.family.presentation.components.FamilySharedBudgetsCard
import com.mascill.keutrack.feature.family.presentation.model.DefaultFamilyInsightsMockContent
import com.mascill.keutrack.feature.family.presentation.model.FamilyInsightsMockContent

private const val FAM_FAB_LIST_CLEARANCE = 72
private const val FAM_TOP_BAR_ELEVATION = 4
private const val FAM_TOP_BAR_PH = 20
private const val FAM_TOP_BAR_PV = 4
private const val FAM_CONTENT_PH = 20
private const val FAM_CONTENT_PT = 8
private const val FAM_CONTENT_PB_EXTRA = 24
private const val FAM_LIST_SECTION_SPACING = 24
private const val FAM_HERO_WIDE_BREAKPOINT = 600

/**
 * Family tab routing to handle screen that will be showing and to handle view model flow /
 * live data collection
 */
@Composable
fun FamilyRouting(
    viewModel: FamilyViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) {
        // Reserved for binding family insights UI state from viewModel.
    }

    FamilyScreen(
        content = DefaultFamilyInsightsMockContent,
        onViewAllHistoryClick = {},
        onAdjustTargetsClick = {},
        onFabClick = {},
    )
}

/**
 * Family Insights — shared spending breakdown, budgets, history (UI-only mock data).
 */
@Composable
fun FamilyScreen(
    content: FamilyInsightsMockContent,
    onViewAllHistoryClick: () -> Unit = {},
    onAdjustTargetsClick: () -> Unit = {},
    onFabClick: () -> Unit = {},
) {
    val pageBg = KeuTrackTheme.contentColors.pageColor
    val semantic = KeuTrackTheme.semanticColors
    val neutral = KeuTrackTheme.neutralColors
    var heroWidth by remember { mutableStateOf(0.dp) }

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
            FloatingActionButton(
                onClick = onFabClick,
                shape = RoundedCornerShape(KeuTrackTheme.shapeTokens.radiusLg),
                backgroundColor = semantic.primary,
                contentColor = neutral.white,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add shared transaction",
                )
            }
        },
    ) { innerPadding ->
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
            item {
                FamilyScreenHeroSection(
                    content = content,
                    heroWidth = heroWidth,
                    onHeroWidthChanged = { heroWidth = it },
                )
            }

            item {
                FamilyHistoryLogSection(
                    content = content,
                    onViewAllClick = onViewAllHistoryClick,
                )
            }

            item {
                FamilySavingTogetherCard(
                    content = content,
                    onAdjustTargetsClick = onAdjustTargetsClick,
                )
            }
        }
    }
}

@Composable
private fun FamilyScreenHeroSection(
    content: FamilyInsightsMockContent,
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
                    content = content,
                    modifier = Modifier.weight(1f),
                )
                FamilySharedBudgetsCard(
                    content = content,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(FAM_LIST_SECTION_SPACING.dp)) {
                FamilyBreakdownCard(content = content)
                FamilySharedBudgetsCard(content = content)
            }
        }
    }
}

@Preview(showBackground = true, name = "Family — Light")
@Composable
private fun FamilyScreenPreview() {
    KeuTrackTheme(darkTheme = false) {
        FamilyScreen(content = DefaultFamilyInsightsMockContent)
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
        FamilyScreen(content = DefaultFamilyInsightsMockContent)
    }
}
