package com.mascill.keutrack.feature.settings.presentation.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.common.utils.PeriodBounds
import com.mascill.keutrack.core.common.utils.PeriodLabels
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.component.KeuTrackModalBottomSheet
import com.mascill.keutrack.core.designsystem.model.KeuTrackButtonStyle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import java.time.LocalDate

private const val SHEET_TITLE = "Mulai siklus"
private const val SHEET_BODY =
    "Pemasukan gajian sering tidak di tanggal 1. Pilih hari mulai, filter Family dan riwayat mengikuti rentang ini setiap periode."
private const val SHEET_PREVIEW_PREFIX = "Periode berjalan: "
private const val SHEET_SAVE = "Simpan"
private const val SHEET_CANCEL = "Batal"
private const val SHEET_PH = 20
private const val SHEET_PB = 16
private const val SHEET_TITLE_PB = 4
private const val SHEET_LIST_MAX_HEIGHT = 280
private const val SHEET_DAY_PV = 10
private const val SHEET_DAY_PH = 12
private val DAY_OPTIONS = (PeriodBounds.MIN_CYCLE_START_DAY..PeriodBounds.MAX_CYCLE_START_DAY).toList()

@Composable
fun SettingsPeriodCycleSheet(
    cycleStartDay: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    KeuTrackModalBottomSheet(onDismissRequest = onDismiss) {
        SettingsPeriodCycleSheetContent(
            cycleStartDay = cycleStartDay,
            onDismiss = onDismiss,
            onSave = onSave,
        )
    }
}

@Composable
internal fun SettingsPeriodCycleSheetContent(
    cycleStartDay: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    var draftDay by rememberSaveable { mutableIntStateOf(cycleStartDay) }
    val listState = rememberLazyListState()
    val preview =
        PeriodLabels.formatPreview(PeriodBounds.containing(LocalDate.now(), draftDay))

    LaunchedEffect(Unit) {
        val index = (draftDay - PeriodBounds.MIN_CYCLE_START_DAY).coerceAtLeast(0)
        listState.scrollToItem(index)
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SHEET_PH.dp)
                .padding(bottom = SHEET_PB.dp)
                .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(
                text = SHEET_TITLE,
                style = typography.headingBold20,
                color = textColors.title,
                modifier = Modifier.padding(bottom = SHEET_TITLE_PB.dp),
            )
            Text(
                text = SHEET_BODY,
                style = typography.bodyRegular14,
                color = textColors.body,
            )
        }
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = SHEET_LIST_MAX_HEIGHT.dp),
        ) {
            items(DAY_OPTIONS, key = { it }) { day ->
                val selected = day == draftDay
                Text(
                    text = "Tanggal $day",
                    style = if (selected) typography.bodyBold16 else typography.bodyRegular16,
                    color = if (selected) semantic.primary else textColors.title,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(shapes.radiusMd))
                            .background(
                                if (selected) {
                                    semantic.primary.copy(alpha = 0.12f)
                                } else {
                                    semantic.surface
                                },
                            )
                            .clickable { draftDay = day }
                            .padding(horizontal = SHEET_DAY_PH.dp, vertical = SHEET_DAY_PV.dp),
                )
            }
        }
        Text(
            text = SHEET_PREVIEW_PREFIX + preview,
            style = typography.bodyRegular14,
            color = textColors.body,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KeuTrackButton(
                text = SHEET_CANCEL,
                onClick = onDismiss,
                style = KeuTrackButtonStyle.Tertiary,
                modifier = Modifier.weight(1f),
            )
            KeuTrackButton(
                text = SHEET_SAVE,
                onClick = { onSave(draftDay) },
                style = KeuTrackButtonStyle.Primary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true, name = "Period cycle sheet — Light")
@Composable
private fun SettingsPeriodCycleSheetLightPreview() {
    KeuTrackTheme(darkTheme = false) {
        SettingsPeriodCycleSheetContent(
            cycleStartDay = 1,
            onDismiss = {},
            onSave = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Period cycle sheet — Dark",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun SettingsPeriodCycleSheetDarkPreview() {
    KeuTrackTheme(darkTheme = true) {
        SettingsPeriodCycleSheetContent(
            cycleStartDay = 25,
            onDismiss = {},
            onSave = {},
        )
    }
}
