package com.mascill.keutrack.feature.transaction.presentation.components

import android.content.res.Configuration
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.feature.transaction.presentation.model.TransactionCategoryIcon
import com.mascill.keutrack.feature.transaction.presentation.model.TransactionRowUi
import kotlin.math.abs
import kotlin.math.roundToInt

private const val REVEAL_ACTION_WIDTH = 72
private const val REVEAL_ACTION_COUNT = 2
private const val REVEAL_VELOCITY_THRESHOLD_DP = 125
private const val REVEAL_POSITION_THRESHOLD = 0.5f
private const val REVEAL_CLOSED_OFFSET = 0f
private const val REVEAL_OFFSET_NONE = 0
private const val REVEAL_PROGRESS_CLOSED = 0f
private const val REVEAL_PROGRESS_OPEN = 1f
private const val REVEAL_CLOSED_TAP_SLOP_DP = 8
private const val REVEAL_EDIT_LABEL = "Ubah"
private const val REVEAL_DELETE_LABEL = "Hapus"
private const val REVEAL_PREVIEW_PADDING = 16

private enum class SwipeRevealValue {
    Closed,
    Open,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeRevealRow(
    revealed: Boolean,
    enabled: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (contentShape: Shape) -> Unit,
) {
    val shapes = KeuTrackTheme.shapeTokens
    val warning = KeuTrackTheme.warningColors
    val danger = KeuTrackTheme.dangerColors
    val density = LocalDensity.current
    val revealPx = with(density) { (REVEAL_ACTION_WIDTH * REVEAL_ACTION_COUNT).dp.toPx() }
    val velocityThresholdPx = with(density) { REVEAL_VELOCITY_THRESHOLD_DP.dp.toPx() }
    val closedTapSlopPx = with(density) { REVEAL_CLOSED_TAP_SLOP_DP.dp.toPx() }
    val cardInteraction = remember { MutableInteractionSource() }

    val state =
        remember {
            AnchoredDraggableState(
                initialValue =
                    if (revealed) SwipeRevealValue.Open else SwipeRevealValue.Closed,
                positionalThreshold = { distance -> distance * REVEAL_POSITION_THRESHOLD },
                velocityThreshold = { velocityThresholdPx },
                snapAnimationSpec = tween(),
                decayAnimationSpec = exponentialDecay(),
            )
        }

    val anchors =
        remember(revealPx) {
            DraggableAnchors {
                SwipeRevealValue.Closed at REVEAL_CLOSED_OFFSET
                SwipeRevealValue.Open at -revealPx
            }
        }

    SideEffect {
        state.updateAnchors(anchors)
    }

    LaunchedEffect(enabled, revealed) {
        if (!enabled) {
            if (state.currentValue != SwipeRevealValue.Closed) {
                state.snapTo(SwipeRevealValue.Closed)
            }
            if (revealed) {
                onRevealedChange(false)
            }
            return@LaunchedEffect
        }
        val target = if (revealed) SwipeRevealValue.Open else SwipeRevealValue.Closed
        if (state.targetValue != target) {
            state.animateTo(target)
        }
    }

    LaunchedEffect(state.currentValue) {
        if (!enabled) return@LaunchedEffect
        val isOpen = state.currentValue == SwipeRevealValue.Open
        if (isOpen != revealed) {
            onRevealedChange(isOpen)
        }
    }

    val offsetPx = state.offset
    val isOpenForTap = enabled && isRevealOpenForTap(offsetPx, revealed, closedTapSlopPx)
    val contentShape =
        swipeContentShape(
            radius = shapes.radiusLg,
            offsetPx = offsetPx,
            revealPx = revealPx,
            enabled = enabled,
        )

    Box(modifier = modifier.fillMaxWidth()) {
        if (enabled) {
            Row(
                modifier =
                    Modifier
                        .matchParentSize()
                        .clip(
                            RoundedCornerShape(
                                topEnd = shapes.radiusLg,
                                bottomEnd = shapes.radiusLg,
                            ),
                        ),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SwipeRevealAction(
                    label = REVEAL_EDIT_LABEL,
                    background = warning.w500,
                    onClick = onEdit,
                )
                SwipeRevealAction(
                    label = REVEAL_DELETE_LABEL,
                    background = danger.d500,
                    onClick = onDelete,
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (enabled) {
                            Modifier.offset {
                                val px = state.offset
                                IntOffset(
                                    if (px.isNaN()) REVEAL_OFFSET_NONE else px.roundToInt(),
                                    REVEAL_OFFSET_NONE,
                                )
                            }
                        } else {
                            Modifier
                        },
                    )
                    .clickable(
                        enabled = isOpenForTap,
                        interactionSource = cardInteraction,
                        indication = ripple(bounded = true),
                        onClick = { onRevealedChange(false) },
                    )
                    .then(
                        if (enabled) {
                            Modifier.anchoredDraggable(
                                state = state,
                                orientation = Orientation.Horizontal,
                            )
                        } else {
                            Modifier
                        },
                    ),
        ) {
            content(contentShape)
        }
    }
}

private fun isRevealOpenForTap(
    offsetPx: Float,
    revealed: Boolean,
    slopPx: Float,
): Boolean {
    if (revealed) {
        return true
    }
    if (offsetPx.isNaN()) {
        return false
    }
    return abs(offsetPx) > slopPx
}

private fun swipeContentShape(
    radius: Dp,
    offsetPx: Float,
    revealPx: Float,
    enabled: Boolean,
): RoundedCornerShape {
    val progress =
        if (!enabled || offsetPx.isNaN() || revealPx <= REVEAL_CLOSED_OFFSET) {
            REVEAL_PROGRESS_CLOSED
        } else {
            (-offsetPx / revealPx).coerceIn(REVEAL_PROGRESS_CLOSED, REVEAL_PROGRESS_OPEN)
        }
    val endRadius = radius * (REVEAL_PROGRESS_OPEN - progress)
    return RoundedCornerShape(
        topStart = radius,
        bottomStart = radius,
        topEnd = endRadius,
        bottomEnd = endRadius,
    )
}

@Composable
private fun SwipeRevealAction(
    label: String,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = KeuTrackTheme.typography
    Box(
        modifier =
            modifier
                .width(REVEAL_ACTION_WIDTH.dp)
                .fillMaxHeight()
                .background(background)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = typography.bodyBold14,
            color = Color.White,
        )
    }
}

@Preview(name = "Reveal — Closed", showBackground = true)
@Preview(
    name = "Reveal — Closed Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SwipeRevealRowClosedPreview() {
    KeuTrackTheme {
        SwipeRevealRowPreviewHost(initiallyRevealed = false)
    }
}

@Preview(name = "Reveal — Open", showBackground = true)
@Preview(
    name = "Reveal — Open Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SwipeRevealRowOpenPreview() {
    KeuTrackTheme {
        SwipeRevealRowPreviewHost(initiallyRevealed = true)
    }
}

@Preview(name = "Reveal — Read only", showBackground = true)
@Preview(
    name = "Reveal — Read only Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SwipeRevealRowReadOnlyPreview() {
    KeuTrackTheme {
        SwipeRevealRowPreviewHost(
            initiallyRevealed = false,
            enabled = false,
            row = previewSwipeRevealRow(canEdit = false),
        )
    }
}

@Composable
private fun SwipeRevealRowPreviewHost(
    initiallyRevealed: Boolean,
    enabled: Boolean = true,
    row: TransactionRowUi = previewSwipeRevealRow(),
) {
    val pageBg = KeuTrackTheme.contentColors.pageColor
    var revealed by remember { mutableStateOf(initiallyRevealed) }
    Box(
        modifier =
            Modifier
                .background(pageBg)
                .padding(REVEAL_PREVIEW_PADDING.dp),
    ) {
        SwipeRevealRow(
            revealed = revealed,
            enabled = enabled,
            onRevealedChange = { revealed = it },
            onEdit = { revealed = false },
            onDelete = {},
        ) { contentShape ->
            TransactionHistoryRow(
                row = row,
                shape = contentShape,
            )
        }
    }
}

private fun previewSwipeRevealRow(canEdit: Boolean = true): TransactionRowUi =
    TransactionRowUi(
        id = "1",
        title = "Bakmi GM Restaurant",
        categoryLabel = "Food & Drinks",
        timeLabel = "12:45 PM",
        amountLabel = "IDR 125.000",
        isExpense = true,
        walletLabel = if (canEdit) "Personal" else "Family",
        categoryIcon = TransactionCategoryIcon.Restaurant,
        syncStatus = SyncStatus.PENDING,
        canEdit = canEdit,
        authorLabel = if (canEdit) null else "Budi",
    )
