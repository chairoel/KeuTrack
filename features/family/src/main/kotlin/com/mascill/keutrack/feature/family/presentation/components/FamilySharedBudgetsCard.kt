package com.mascill.keutrack.feature.family.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.family.presentation.model.FamilyBudgetBarTone
import com.mascill.keutrack.feature.family.presentation.model.FamilyBudgetRowUi
import com.mascill.keutrack.feature.family.presentation.model.FamilyInsightsMockContent

private const val FAM_BUDGET_SECTION_PH = 24
private const val FAM_BUDGET_SECTION_PV = 24
private const val FAM_BUDGET_ROW_SPACING = 24
private const val FAM_BUDGET_ROW_INNER = 8
private const val FAM_BUDGET_FOOTNOTE_PT = 4
private const val FAM_BUDGET_MUTED_ALPHA = 0.8f
private const val FAM_BUDGET_SPENT_CAP_SEPARATOR = " / "

@Composable
fun FamilySharedBudgetsCard(
    content: FamilyInsightsMockContent,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val effects = KeuTrackTheme.effectTokens
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val success = KeuTrackTheme.successColors

    val shape = RoundedCornerShape(shapes.radiusXl)

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
                .padding(horizontal = FAM_BUDGET_SECTION_PH.dp, vertical = FAM_BUDGET_SECTION_PV.dp),
    ) {
        Text(
            text = content.sharedBudgetsTitle,
            style = typography.headingBold20,
            color = textColors.title,
        )
        Spacer(modifier = Modifier.height(FAM_BUDGET_ROW_SPACING.dp))
        Column(verticalArrangement = Arrangement.spacedBy(FAM_BUDGET_ROW_SPACING.dp)) {
            content.budgetRows.forEach { row ->
                FamilyBudgetRow(
                    row = row,
                    successBrush =
                        Brush.horizontalGradient(
                            colors = listOf(semantic.secondary, success.s700),
                        ),
                    errorBrush =
                        Brush.horizontalGradient(
                            colors = listOf(semantic.error, semantic.tertiary),
                        ),
                )
            }
        }
    }
}

@Composable
private fun FamilyBudgetRow(
    row: FamilyBudgetRowUi,
    successBrush: Brush,
    errorBrush: Brush,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors

    val footnoteColor =
        when (row.tone) {
            FamilyBudgetBarTone.Success -> semantic.secondary
            FamilyBudgetBarTone.Error -> semantic.error
            FamilyBudgetBarTone.Primary -> semantic.onSurfaceVariant
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (row.muted) Modifier.alpha(FAM_BUDGET_MUTED_ALPHA) else Modifier),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = row.title,
                style = typography.bodyBold16,
                color = textColors.title,
            )
            Text(
                text = row.spentLabel + FAM_BUDGET_SPENT_CAP_SEPARATOR + row.capLabel,
                style = typography.bodyRegular12,
                color = semantic.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(FAM_BUDGET_ROW_INNER.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(shapes.progressThickness)
                    .clip(RoundedCornerShape(shapes.radiusXl))
                    .background(semantic.surfaceContainerHighest),
        ) {
            val fillBrush =
                when (row.tone) {
                    FamilyBudgetBarTone.Success -> successBrush
                    FamilyBudgetBarTone.Error -> errorBrush
                    FamilyBudgetBarTone.Primary ->
                        Brush.horizontalGradient(
                            colors = listOf(semantic.primary, semantic.primaryContainer),
                        )
                }
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(row.progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(shapes.radiusXl))
                        .background(fillBrush),
            )
        }
        row.footnote?.let { note ->
            Text(
                text = note,
                style = typography.bodyBold10,
                color = footnoteColor,
                modifier = Modifier.padding(top = FAM_BUDGET_FOOTNOTE_PT.dp),
            )
        }
    }
}
