package com.mascill.keutrack.feature.family.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.format.CurrencyFormat
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.family.presentation.model.FamilySpendSegment

private const val FAM_DONUT_STROKE_RATIO = 0.14f
private const val FAM_DONUT_HOLE_RATIO = 0.55f
private const val FAM_BREAKDOWN_INNER_GAP = 24
private const val FAM_BREAKDOWN_LEGEND_DOT = 12
private const val FAM_BREAKDOWN_LEGEND_GAP = 12
private const val FAM_BREAKDOWN_LABEL_PT = 4
private const val FAM_BREAKDOWN_WIDE_MIN_WIDTH = 400
private const val FAM_BREAKDOWN_LABEL_ALPHA = 0.6f
private const val FAM_BREAKDOWN_LEGEND_MAX_WIDTH = 220
private const val FAM_BREAKDOWN_LEGEND_DOT_TOP_PT = 4
private const val FAM_DONUT_START_ANGLE_DEG = -90f
private const val FAM_DONUT_FULL_CIRCLE_DEG = 360f
private const val FAM_PULSE_LABEL = "Pulse Report"
private const val FAM_BREAKDOWN_TITLE = "Family Breakdown"
private const val FAM_MONTHLY_TOTAL_LABEL = "Monthly Total"
private const val FAM_EMPTY_SEGMENTS =
    "Belum ada pengeluaran bulan ini. Catat transaksi di dompet keluarga untuk melihat breakdown."
private val FAM_DONUT_SIZE = 176.dp

@Composable
fun FamilyBreakdownCard(
    monthlyTotalExpense: Long,
    spendSegments: List<FamilySpendSegment>,
    modifier: Modifier = Modifier,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val semantic = KeuTrackTheme.semanticColors
    val density = LocalDensity.current
    var layoutWidth by remember { mutableStateOf(0.dp) }
    val monthlyTotalAmount = CurrencyFormat.formatIdr(monthlyTotalExpense)

    KeuTrackCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = FAM_PULSE_LABEL,
                style = typography.bodyBold10,
                color = semantic.primary.copy(alpha = FAM_BREAKDOWN_LABEL_ALPHA),
                modifier = Modifier.padding(bottom = FAM_BREAKDOWN_LABEL_PT.dp),
            )
            Text(
                text = FAM_BREAKDOWN_TITLE,
                style = typography.headingBold20,
                color = textColors.title,
            )
            Spacer(modifier = Modifier.height(FAM_BREAKDOWN_INNER_GAP.dp))

            if (spendSegments.isEmpty()) {
                Text(
                    text = FAM_EMPTY_SEGMENTS,
                    style = typography.bodyRegular14,
                    color = semantic.onSurfaceVariant,
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                layoutWidth = with(density) { coords.size.width.toDp() }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    val wide = layoutWidth >= FAM_BREAKDOWN_WIDE_MIN_WIDTH.dp
                    if (wide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            FamilyDonutChart(
                                segments = spendSegments,
                                monthlyTotalLabel = FAM_MONTHLY_TOTAL_LABEL,
                                monthlyTotalAmount = monthlyTotalAmount,
                                modifier = Modifier.size(FAM_DONUT_SIZE),
                            )
                            Column(
                                modifier =
                                    Modifier
                                        .weight(1f, fill = false)
                                        .widthIn(max = FAM_BREAKDOWN_LEGEND_MAX_WIDTH.dp)
                                        .padding(start = FAM_BREAKDOWN_INNER_GAP.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(FAM_BREAKDOWN_LEGEND_GAP.dp),
                            ) {
                                spendSegments.forEachIndexed { index, segment ->
                                    FamilySpendLegendRow(
                                        segment = segment,
                                        color = segmentColor(index),
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.spacedBy(FAM_BREAKDOWN_INNER_GAP.dp),
                        ) {
                            FamilyDonutChart(
                                segments = spendSegments,
                                monthlyTotalLabel = FAM_MONTHLY_TOTAL_LABEL,
                                monthlyTotalAmount = monthlyTotalAmount,
                                modifier = Modifier.size(FAM_DONUT_SIZE),
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement =
                                    Arrangement.spacedBy(FAM_BREAKDOWN_LEGEND_GAP.dp),
                            ) {
                                spendSegments.forEachIndexed { index, segment ->
                                    FamilySpendLegendRow(
                                        segment = segment,
                                        color = segmentColor(index),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun segmentColor(index: Int): androidx.compose.ui.graphics.Color {
    val semantic = KeuTrackTheme.semanticColors
    val colors =
        listOf(
            semantic.primary,
            semantic.secondary,
            semantic.tertiary,
            semantic.primary.copy(alpha = 0.55f),
            semantic.secondary.copy(alpha = 0.55f),
        )
    return colors[index % colors.size]
}

@Composable
private fun FamilySpendLegendRow(
    segment: FamilySpendSegment,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val semantic = KeuTrackTheme.semanticColors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(FAM_BREAKDOWN_LEGEND_GAP.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = FAM_BREAKDOWN_LEGEND_DOT_TOP_PT.dp)
                    .size(FAM_BREAKDOWN_LEGEND_DOT.dp)
                    .clip(CircleShape)
                    .background(color),
        )
        Column {
            Text(
                text = segment.label,
                style = typography.bodyBold16,
                color = textColors.title,
            )
            Text(
                text = segment.detail,
                style = typography.bodyRegular12,
                color = semantic.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FamilyDonutChart(
    segments: List<FamilySpendSegment>,
    monthlyTotalLabel: String,
    monthlyTotalAmount: String,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography
    val colors =
        listOf(
            semantic.primary,
            semantic.secondary,
            semantic.tertiary,
            semantic.primary.copy(alpha = 0.55f),
            semantic.secondary.copy(alpha = 0.55f),
        )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * FAM_DONUT_STROKE_RATIO
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            var start = FAM_DONUT_START_ANGLE_DEG
            segments.forEachIndexed { index, seg ->
                val sweep = FAM_DONUT_FULL_CIRCLE_DEG * seg.fraction
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize(FAM_DONUT_HOLE_RATIO)
                    .clip(CircleShape)
                    .background(semantic.surfaceContainerLowest),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = monthlyTotalLabel,
                    style = typography.bodyRegular12,
                    color = semantic.onSurfaceVariant,
                )
                Text(
                    text = monthlyTotalAmount,
                    style = typography.bodyBold16,
                    color = semantic.primary,
                )
            }
        }
    }
}
