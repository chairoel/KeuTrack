package com.mascill.keutrack.feature.family.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.family.presentation.model.FamilyHistoryCategoryIcon
import com.mascill.keutrack.feature.family.presentation.model.FamilyHistoryRowUi
import com.mascill.keutrack.feature.family.presentation.model.FamilyMemberAttribution
import com.mascill.keutrack.feature.family.presentation.model.toImageVector

private const val FAM_HIST_ROW_PH = 14
private const val FAM_HIST_ROW_PV = 12
private const val FAM_HIST_ICON_BOX = 48
private const val FAM_HIST_ICON_SIZE = 24
private const val FAM_HIST_MIDDLE_PH = 12
private const val FAM_HIST_SUBTITLE_PT = 2
private const val FAM_HIST_PILL_PT = 6
private const val FAM_HIST_PILL_PH = 8
private const val FAM_HIST_PILL_PV = 4
private const val FAM_HIST_DOT = 6
private const val FAM_HIST_PILL_ICON_SPACING = 6
private const val FAM_HIST_ICON_BG_ALPHA_PRIMARY = 0.12f
private const val FAM_HIST_ICON_BG_ALPHA_TERTIARY = 0.15f
private const val FAM_HIST_ADDED_BY_WIFE = "Added by Istri"
private const val FAM_HIST_ADDED_BY_HUSBAND = "Added by Suami"

@Composable
fun FamilyHistoryRow(
    row: FamilyHistoryRowUi,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val success = KeuTrackTheme.successColors

    val (iconBg, iconTint) =
        when (row.categoryIcon) {
            FamilyHistoryCategoryIcon.Groceries ->
                success.s100 to semantic.secondary
            FamilyHistoryCategoryIcon.Utilities ->
                semantic.primary.copy(alpha = FAM_HIST_ICON_BG_ALPHA_PRIMARY) to semantic.primary
            FamilyHistoryCategoryIcon.School ->
                semantic.tertiary.copy(alpha = FAM_HIST_ICON_BG_ALPHA_TERTIARY) to semantic.tertiary
        }

    KeuTrackCard(
        modifier = modifier,
        contentPadding =
            PaddingValues(
                horizontal = FAM_HIST_ROW_PH.dp,
                vertical = FAM_HIST_ROW_PV.dp,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(FAM_HIST_ICON_BOX.dp)
                        .clip(RoundedCornerShape(shapes.radiusMd))
                        .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = row.categoryIcon.toImageVector(),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(FAM_HIST_ICON_SIZE.dp),
                )
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = FAM_HIST_MIDDLE_PH.dp),
            ) {
                Text(
                    text = row.title,
                    style = typography.bodyBold16,
                    color = textColors.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.subtitle,
                    style = typography.bodyRegular12,
                    color = semantic.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = FAM_HIST_SUBTITLE_PT.dp),
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = row.amountLabel,
                    style = typography.bodyBold14,
                    color = textColors.title,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
                FamilyAddedByPill(
                    addedBy = row.addedBy,
                    modifier = Modifier.padding(top = FAM_HIST_PILL_PT.dp),
                )
            }
        }
    }
}

@Composable
private fun FamilyAddedByPill(
    addedBy: FamilyMemberAttribution,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors

    val label =
        when (addedBy) {
            FamilyMemberAttribution.Wife -> FAM_HIST_ADDED_BY_WIFE
            FamilyMemberAttribution.Husband -> FAM_HIST_ADDED_BY_HUSBAND
        }

    val dotColor =
        when (addedBy) {
            FamilyMemberAttribution.Wife -> semantic.secondary
            FamilyMemberAttribution.Husband -> semantic.primary
        }

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(shapes.radiusMd))
                .background(semantic.surfaceContainerHigh)
                .padding(horizontal = FAM_HIST_PILL_PH.dp, vertical = FAM_HIST_PILL_PV.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FAM_HIST_PILL_ICON_SPACING.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(FAM_HIST_DOT.dp)
                    .clip(CircleShape)
                    .background(dotColor),
        )
        Text(
            text = label,
            style = typography.bodyBold10,
            color = textColors.title,
            maxLines = 1,
        )
    }
}
