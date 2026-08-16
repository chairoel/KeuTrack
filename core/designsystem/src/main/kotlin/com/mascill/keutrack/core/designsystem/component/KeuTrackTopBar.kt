package com.mascill.keutrack.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.model.KeuTrackTopBarTitleAlignment
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

/** Minimum bar height (Material `IconButton` touch target). */
private val KeuTrackTopBarMinHeight = 48.dp

@Composable
fun KeuTrackTopBar(
    title: String,
    modifier: Modifier = Modifier,
    titleAlignment: KeuTrackTopBarTitleAlignment = KeuTrackTopBarTitleAlignment.Center,
    leading: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    KeuTrackTopBar(
        modifier = modifier,
        titleAlignment = titleAlignment,
        leading = leading,
        trailing = trailing,
        title = {
            Text(
                text = title,
                style = typography.headingBold20,
                color = textColors.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign =
                    when (titleAlignment) {
                        KeuTrackTopBarTitleAlignment.Start -> TextAlign.Start
                        KeuTrackTopBarTitleAlignment.Center -> TextAlign.Center
                        KeuTrackTopBarTitleAlignment.End -> TextAlign.End
                    },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

@Composable
fun KeuTrackTopBar(
    modifier: Modifier = Modifier,
    titleAlignment: KeuTrackTopBarTitleAlignment = KeuTrackTopBarTitleAlignment.Center,
    leading: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
    title: @Composable () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = KeuTrackTopBarMinHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (titleAlignment) {
            KeuTrackTopBarTitleAlignment.Center -> {
                CenteredTopBarContent(
                    leading = leading,
                    trailing = trailing,
                    title = title,
                )
            }

            KeuTrackTopBarTitleAlignment.Start -> {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        leading()
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            title()
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    trailing()
                }
            }

            KeuTrackTopBarTitleAlignment.End -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    leading()
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    title()
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    trailing()
                }
            }
        }
    }
}

/**
 * Optically centers [title] and insets it by the wider of [leading] / [trailing],
 * so a back button does not steal a full third of the bar.
 */
@Composable
private fun RowScope.CenteredTopBarContent(
    leading: @Composable RowScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit,
    title: @Composable () -> Unit,
) {
    var leadingWidthPx by remember { mutableIntStateOf(0) }
    var trailingWidthPx by remember { mutableIntStateOf(0) }
    val sideInset = with(LocalDensity.current) { maxOf(leadingWidthPx, trailingWidthPx).toDp() }

    Box(modifier = Modifier.weight(1f)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = sideInset),
            contentAlignment = Alignment.Center,
        ) {
            title()
        }
        Row(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .onSizeChanged { leadingWidthPx = it.width },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
        }
        Row(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .onSizeChanged { trailingWidthPx = it.width },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            trailing()
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun KeuTrackTopBarPreview() {
    KeuTrackTheme {
        Surface(color = KeuTrackTheme.contentColors.pageColor) {
            KeuTrackTopBar(
                title = "Settings",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
    }
}

@Preview(name = "Back + long title", showBackground = true)
@Composable
private fun KeuTrackTopBarBackPreview() {
    KeuTrackTheme {
        Surface(color = KeuTrackTheme.contentColors.pageColor) {
            KeuTrackTopBar(
                title = "Riwayat Keluarga",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                leading = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        }
    }
}
