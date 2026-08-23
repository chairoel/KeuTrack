package com.mascill.keutrack.feature.family.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val FAM_STEPPER_PH = 8
private const val FAM_STEPPER_PV = 4
private const val FAM_PREV_MONTH_DESC = "Bulan sebelumnya"
private const val FAM_NEXT_MONTH_DESC = "Bulan berikutnya"

@Composable
fun FamilyMonthStepper(
    monthLabel: String,
    canSelectPreviousMonth: Boolean,
    canSelectNextMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val effects = KeuTrackTheme.effectTokens
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val shape = RoundedCornerShape(shapes.radiusMd)

    Row(
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
                .padding(horizontal = FAM_STEPPER_PH.dp, vertical = FAM_STEPPER_PV.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPreviousMonth,
            enabled = canSelectPreviousMonth,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = FAM_PREV_MONTH_DESC,
                tint =
                    if (canSelectPreviousMonth) {
                        semantic.onSurface
                    } else {
                        semantic.onSurfaceVariant
                    },
            )
        }
        Text(
            text = monthLabel,
            style = typography.headingBold20,
            color = textColors.title,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onNextMonth,
            enabled = canSelectNextMonth,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = FAM_NEXT_MONTH_DESC,
                tint =
                    if (canSelectNextMonth) {
                        semantic.onSurface
                    } else {
                        semantic.onSurfaceVariant
                    },
            )
        }
    }
}

@Preview(showBackground = true, name = "Month stepper — current")
@Composable
private fun FamilyMonthStepperPreview() {
    KeuTrackTheme(darkTheme = false) {
        FamilyMonthStepper(
            monthLabel = "Agustus 2026",
            canSelectPreviousMonth = true,
            canSelectNextMonth = false,
            onPreviousMonth = {},
            onNextMonth = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(
    showBackground = true,
    name = "Month stepper — past dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun FamilyMonthStepperDarkPreview() {
    KeuTrackTheme(darkTheme = true) {
        FamilyMonthStepper(
            monthLabel = "Juli 2026",
            canSelectPreviousMonth = true,
            canSelectNextMonth = true,
            onPreviousMonth = {},
            onNextMonth = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
