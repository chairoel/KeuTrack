package com.mascill.keutrack.feature.family.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.model.KeuTrackButtonStyle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.family.presentation.model.FamilyInsightsMockContent

private const val FAM_INSIGHT_PH = 24
private const val FAM_INSIGHT_PV = 24
private const val FAM_INSIGHT_BODY_PT = 8
private const val FAM_INSIGHT_CTA_PT = 16
private const val FAM_INSIGHT_DECO_OFFSET_X = 200
private const val FAM_INSIGHT_DECO_OFFSET_Y = -80
private const val FAM_INSIGHT_DECO_SIZE = 160
private const val FAM_INSIGHT_DECO_ALPHA = 0.1f
private const val FAM_INSIGHT_BODY_TEXT_ALPHA = 0.88f

@Composable
fun FamilySavingTogetherCard(
    content: FamilyInsightsMockContent,
    onAdjustTargetsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val typography = KeuTrackTheme.typography
    val neutral = KeuTrackTheme.neutralColors

    val shape = RoundedCornerShape(shapes.radiusXl)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(semantic.primary),
    ) {
        Box(
            modifier =
                Modifier
                    .offset(x = FAM_INSIGHT_DECO_OFFSET_X.dp, y = FAM_INSIGHT_DECO_OFFSET_Y.dp)
                    .size(FAM_INSIGHT_DECO_SIZE.dp)
                    .clip(CircleShape)
                    .background(neutral.white.copy(alpha = FAM_INSIGHT_DECO_ALPHA)),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FAM_INSIGHT_PH.dp, vertical = FAM_INSIGHT_PV.dp),
        ) {
            Text(
                text = content.insightTitle,
                style = typography.headingBold20,
                color = neutral.white,
            )
            Text(
                text = content.insightBody,
                style = typography.bodyRegular14,
                color = neutral.white.copy(alpha = FAM_INSIGHT_BODY_TEXT_ALPHA),
                modifier = Modifier.padding(top = FAM_INSIGHT_BODY_PT.dp),
            )
            Spacer(modifier = Modifier.height(FAM_INSIGHT_CTA_PT.dp))
            KeuTrackButton(
                text = content.insightCtaLabel,
                onClick = onAdjustTargetsClick,
                style = KeuTrackButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
