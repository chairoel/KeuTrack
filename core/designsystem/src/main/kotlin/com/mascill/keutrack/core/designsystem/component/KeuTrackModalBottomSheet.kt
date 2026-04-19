package com.mascill.keutrack.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

/**
 * Modal bottom sheet using Material3 [ModalBottomSheet] with KeuTrack surface shape and colors.
 * Inner content should use [KeuTrackTheme] typography and colors (Material2); only the sheet chrome
 * comes from Material3.
 * Uses an internal remembered sheet state. Public signature avoids experimental Material3 types so
 * feature modules do not need [ExperimentalMaterial3Api] opt-in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeuTrackModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrimColor: Color = Color.Black.copy(alpha = 0.5f),
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val topShape =
        RoundedCornerShape(
            topStart = shapes.radiusXl,
            topEnd = shapes.radiusXl,
        )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = topShape,
        containerColor = semantic.surface,
        contentColor = semantic.onSurface,
        tonalElevation = 0.dp,
        scrimColor = scrimColor,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        semantic.onSurfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(2.dp),
                    ),
            )
        },
        content = { content() },
    )
}
