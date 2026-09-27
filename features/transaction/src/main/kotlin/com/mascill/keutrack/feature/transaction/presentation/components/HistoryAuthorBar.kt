package com.mascill.keutrack.feature.transaction.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryAuthorOption

private const val BAR_CHIP_SPACING = 8
private const val BAR_CHIP_PH = 12
private const val BAR_CHIP_PV = 8

@Composable
fun HistoryAuthorBar(
    options: List<HistoryAuthorOption>,
    selectedUserId: String?,
    onAuthorSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(BAR_CHIP_SPACING.dp),
    ) {
        options.forEach { option ->
            AuthorChip(
                label = option.label,
                selected = option.userId == selectedUserId,
                onClick = { onAuthorSelected(option.userId) },
            )
        }
    }
}

@Composable
private fun AuthorChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val semantic = KeuTrackTheme.semanticColors
    val effects = KeuTrackTheme.effectTokens
    val typography = KeuTrackTheme.typography
    val shape = RoundedCornerShape(percent = 50)
    val background = if (selected) semantic.primary else semantic.surfaceContainerHigh
    val contentColor = if (selected) Color.White else semantic.onSurface

    Text(
        text = label,
        style = typography.bodyBold12,
        color = contentColor,
        modifier =
            Modifier
                .clip(shape)
                .background(background)
                .then(
                    if (selected) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = effects.ghostBorderWidth,
                            color = effects.ghostBorderColor,
                            shape = shape,
                        )
                    },
                )
                .clickable(onClick = onClick)
                .padding(horizontal = BAR_CHIP_PH.dp, vertical = BAR_CHIP_PV.dp),
    )
}

@Preview(showBackground = true, name = "Author bar — Semua")
@Composable
private fun HistoryAuthorBarPreview() {
    KeuTrackTheme(darkTheme = false) {
        HistoryAuthorBar(
            options = previewAuthorOptions(),
            selectedUserId = null,
            onAuthorSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(
    showBackground = true,
    name = "Author bar — Saya dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HistoryAuthorBarSayaDarkPreview() {
    KeuTrackTheme(darkTheme = true) {
        HistoryAuthorBar(
            options = previewAuthorOptions(),
            selectedUserId = "user-1",
            onAuthorSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun previewAuthorOptions(): List<HistoryAuthorOption> =
    listOf(
        HistoryAuthorOption(userId = null, label = "Semua"),
        HistoryAuthorOption(userId = "user-1", label = "Saya"),
        HistoryAuthorOption(userId = "user-2", label = "Budi"),
        HistoryAuthorOption(userId = "user-3", label = "Siti"),
    )
