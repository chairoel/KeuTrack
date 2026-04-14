package com.mascill.keutrack.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        leading()
                    }
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    title()
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        trailing()
                    }
                }
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

//@Preview(showBackground = true, name = "Title — center")
//@Composable
//private fun KeuTrackTopBarPreviewCenter() {
//    KeuTrackTheme(darkTheme = false) {
//        Surface(color = KeuTrackTheme.contentColors.pageColor) {
//            KeuTrackTopBar(
//                title = "Settings",
//                modifier =
//                    Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 20.dp, vertical = 4.dp),
//            )
//        }
//    }
//}
//
//@Preview(showBackground = true, name = "Title — start (brand + actions)")
//@Composable
//private fun KeuTrackTopBarPreviewStart() {
//    KeuTrackTheme(darkTheme = false) {
//        Surface(color = KeuTrackTheme.contentColors.pageColor) {
//            KeuTrackTopBar(
//                modifier =
//                    Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 20.dp, vertical = 4.dp),
//                titleAlignment = KeuTrackTopBarTitleAlignment.Start,
//                leading = {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Text(
//                            text = "◎",
//                            style = KeuTrackTheme.typography.headingBold20,
//                            color = KeuTrackTheme.textColors.title,
//                            modifier = Modifier.padding(end = 8.dp),
//                        )
//                        Text(
//                            text = "KeuTrack",
//                            style = KeuTrackTheme.typography.headingBold20,
//                            color = KeuTrackTheme.textColors.title,
//                        )
//                    }
//                },
//                title = {},
//                trailing = {
//                    Text(
//                        text = "···",
//                        style = KeuTrackTheme.typography.headingBold20,
//                        color = KeuTrackTheme.semanticColors.onSurface,
//                        modifier = Modifier.padding(horizontal = 12.dp),
//                    )
//                },
//            )
//        }
//    }
//}
//
//@Preview(showBackground = true, name = "Title — end")
//@Composable
//private fun KeuTrackTopBarPreviewEnd() {
//    KeuTrackTheme(darkTheme = false) {
//        Surface(color = KeuTrackTheme.contentColors.pageColor) {
//            KeuTrackTopBar(
//                title = "Export",
//                titleAlignment = KeuTrackTopBarTitleAlignment.End,
//                modifier =
//                    Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 20.dp, vertical = 4.dp),
//                leading = {
//                    Text(
//                        text = "←",
//                        style = KeuTrackTheme.typography.headingBold20,
//                        color = KeuTrackTheme.semanticColors.onSurface,
//                        modifier = Modifier.padding(end = 8.dp),
//                    )
//                },
//            )
//        }
//    }
//}
//
//@Preview(showBackground = true, name = "Custom title composable")
//@Composable
//private fun KeuTrackTopBarPreviewCustomTitle() {
//    KeuTrackTheme(darkTheme = false) {
//        Surface(color = KeuTrackTheme.contentColors.pageColor) {
//            KeuTrackTopBar(
//                modifier =
//                    Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 20.dp, vertical = 4.dp),
//                title = {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Text(
//                            text = "Step ",
//                            style = KeuTrackTheme.typography.bodyRegular14,
//                            color = KeuTrackTheme.textColors.body,
//                        )
//                        Text(
//                            text = "2",
//                            style = KeuTrackTheme.typography.headingBold20,
//                            color = KeuTrackTheme.textColors.title,
//                        )
//                        Text(
//                            text = "/4",
//                            style = KeuTrackTheme.typography.bodyRegular14,
//                            color = KeuTrackTheme.textColors.body,
//                        )
//                    }
//                },
//            )
//        }
//    }
//}
//
//@Preview(showBackground = true, name = "Title — center (dark)")
//@Composable
//private fun KeuTrackTopBarPreviewCenterDark() {
//    KeuTrackTheme(darkTheme = true) {
//        Surface(color = KeuTrackTheme.contentColors.pageColor) {
//            KeuTrackTopBar(
//                title = "Settings",
//                modifier =
//                    Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 20.dp, vertical = 4.dp),
//            )
//        }
//    }
//}
//
//@Preview(
//    showBackground = true,
//    name = "Title — center (landscape)",
//    device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape",
//)
//@Composable
//private fun KeuTrackTopBarPreviewCenterLandscape() {
//    KeuTrackTopBarPreviewCenter()
//}
