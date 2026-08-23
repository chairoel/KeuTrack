package com.mascill.keutrack.feature.transaction.presentation.history

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.component.KeuTrackTopBar
import com.mascill.keutrack.core.designsystem.component.snackbar.KeuTrackInlineSnackbar
import com.mascill.keutrack.core.designsystem.model.KeuTrackButtonStyle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.feature.transaction.presentation.components.DatePickerDialogHost
import com.mascill.keutrack.feature.transaction.presentation.components.HistoryPeriodBar
import com.mascill.keutrack.feature.transaction.presentation.components.TransactionHistoryRow
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryPeriodPreset
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryScope
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryUIState
import com.mascill.keutrack.feature.transaction.presentation.model.TransactionCategoryIcon
import com.mascill.keutrack.feature.transaction.presentation.model.TransactionRowUi
import java.time.LocalDate

private const val HISTORY_TITLE = "Riwayat"
private const val HISTORY_PERSONAL_TITLE = "Riwayat Personal"
private const val HISTORY_FAMILY_TITLE = "Riwayat Keluarga"
private const val HISTORY_EMPTY_TITLE = "Belum ada transaksi"
private const val HISTORY_EMPTY_BODY = "Catat pemasukan atau pengeluaran pertamamu."
private const val HISTORY_PERSONAL_EMPTY_TITLE = "Belum ada transaksi personal"
private const val HISTORY_PERSONAL_EMPTY_BODY =
    "Belum ada transaksi di dompet personal. Gunakan tombol di bawah untuk menambah transaksi."
private const val HISTORY_FAMILY_EMPTY_TITLE = "Belum ada transaksi keluarga"
private const val HISTORY_FAMILY_EMPTY_BODY =
    "Belum ada transaksi di dompet keluarga. Gunakan tombol di bawah untuk menambah transaksi bersama."
private const val HISTORY_FILTERED_EMPTY_TITLE = "Tidak ada transaksi di periode ini"
private const val HISTORY_FILTERED_EMPTY_BODY = "Coba ubah filter tanggal."
private const val HISTORY_FILTERED_EMPTY_CTA = "Ubah ke Semua"
private const val HISTORY_EMPTY_CTA = "Tambah transaksi"
private const val HISTORY_ERROR_DISMISS = "Dismiss"
private const val HISTORY_TOP_BAR_ELEVATION = 4
private const val HISTORY_TOP_BAR_PH = 8
private const val HISTORY_TOP_BAR_PV = 4
private const val HISTORY_CONTENT_PH = 20
private const val HISTORY_CONTENT_PT = 8
private const val HISTORY_CONTENT_PB = 24
private const val HISTORY_ROW_SPACING = 10
private const val HISTORY_EMPTY_SPACING = 12

private enum class HistoryCustomPickerStep { From, To }

@Composable
fun TransactionHistoryScreen(
    uiState: HistoryUIState,
    onBack: () -> Unit,
    onAddTransaction: () -> Unit,
    onDismissError: () -> Unit = {},
    onPeriodPresetSelected: (HistoryPeriodPreset) -> Unit = {},
    onCustomRangeConfirmed: (LocalDate, LocalDate) -> Unit = { _, _ -> },
    onClearPeriodFilter: () -> Unit = {},
) {
    val pageBg = KeuTrackTheme.contentColors.pageColor
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val today = LocalDate.now()
    var customPickerStep by remember { mutableStateOf<HistoryCustomPickerStep?>(null) }
    var pendingCustomFrom by remember { mutableStateOf<LocalDate?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            backgroundColor = pageBg,
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = pageBg,
                    elevation = HISTORY_TOP_BAR_ELEVATION.dp,
                ) {
                    KeuTrackTopBar(
                        title = historyTitle(uiState.scope),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(
                                    horizontal = HISTORY_TOP_BAR_PH.dp,
                                    vertical = HISTORY_TOP_BAR_PV.dp,
                                ),
                        leading = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = semantic.onSurface,
                                )
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            ) {
                HistoryPeriodBar(
                    selectedPreset = uiState.periodPreset,
                    customRangeLabel =
                        uiState.periodSummaryLabel.takeIf {
                            uiState.periodPreset == HistoryPeriodPreset.Custom
                        },
                    rangeError = uiState.periodRangeError,
                    onPresetSelected = { preset ->
                        if (preset == HistoryPeriodPreset.Custom) {
                            pendingCustomFrom = uiState.customFrom ?: today
                            customPickerStep = HistoryCustomPickerStep.From
                        } else {
                            onPeriodPresetSelected(preset)
                        }
                    },
                    modifier =
                        Modifier.padding(
                            start = HISTORY_CONTENT_PH.dp,
                            end = HISTORY_CONTENT_PH.dp,
                            top = HISTORY_CONTENT_PT.dp,
                        ),
                )
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = semantic.primary)
                        }
                    }

                    uiState.items.isEmpty() -> {
                        HistoryEmptyContent(
                            uiState = uiState,
                            onAddTransaction = onAddTransaction,
                            onClearPeriodFilter = onClearPeriodFilter,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding =
                                PaddingValues(
                                    start = HISTORY_CONTENT_PH.dp,
                                    end = HISTORY_CONTENT_PH.dp,
                                    top = HISTORY_CONTENT_PT.dp,
                                    bottom = HISTORY_CONTENT_PB.dp,
                                ),
                            verticalArrangement = Arrangement.spacedBy(HISTORY_ROW_SPACING.dp),
                        ) {
                            items(uiState.items, key = { it.id }) { row ->
                                TransactionHistoryRow(row = row)
                            }
                        }
                    }
                }
            }
        }

        uiState.errorMessage?.let { message ->
            KeuTrackInlineSnackbar(
                message = message,
                onDismiss = onDismissError,
                actionLabel = HISTORY_ERROR_DISMISS,
            )
        }
    }

    DatePickerDialogHost(
        visible = customPickerStep == HistoryCustomPickerStep.From,
        selectedDate = pendingCustomFrom ?: uiState.customFrom ?: today,
        onDateSelected = { date ->
            pendingCustomFrom = date
            customPickerStep = HistoryCustomPickerStep.To
        },
        onDismiss = {
            if (customPickerStep == HistoryCustomPickerStep.From) {
                customPickerStep = null
                pendingCustomFrom = null
            }
        },
        maxDate = today,
    )
    DatePickerDialogHost(
        visible = customPickerStep == HistoryCustomPickerStep.To,
        selectedDate = uiState.customTo ?: today,
        onDateSelected = { date ->
            val from = pendingCustomFrom ?: today
            onCustomRangeConfirmed(from, date)
            customPickerStep = null
            pendingCustomFrom = null
        },
        onDismiss = {
            if (customPickerStep == HistoryCustomPickerStep.To) {
                customPickerStep = null
                pendingCustomFrom = null
            }
        },
        minDate = pendingCustomFrom,
        maxDate = today,
    )
}

@Composable
private fun HistoryEmptyContent(
    uiState: HistoryUIState,
    onAddTransaction: () -> Unit,
    onClearPeriodFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val filtered = uiState.hasActivePeriodFilter
    Column(
        modifier = modifier.padding(horizontal = HISTORY_CONTENT_PH.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (filtered) HISTORY_FILTERED_EMPTY_TITLE else historyEmptyTitle(uiState.scope),
            style = typography.headingBold20,
            color = textColors.title,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (filtered) HISTORY_FILTERED_EMPTY_BODY else historyEmptyBody(uiState.scope),
            style = typography.bodyRegular14,
            color = textColors.body,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = HISTORY_EMPTY_SPACING.dp),
        )
        if (filtered) {
            KeuTrackButton(
                text = HISTORY_FILTERED_EMPTY_CTA,
                onClick = onClearPeriodFilter,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = (HISTORY_EMPTY_SPACING * 2).dp),
            )
            KeuTrackButton(
                text = HISTORY_EMPTY_CTA,
                onClick = onAddTransaction,
                style = KeuTrackButtonStyle.Tertiary,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = HISTORY_EMPTY_SPACING.dp),
            )
        } else {
            KeuTrackButton(
                text = HISTORY_EMPTY_CTA,
                onClick = onAddTransaction,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = (HISTORY_EMPTY_SPACING * 2).dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionHistoryScreenPreview() {
    KeuTrackTheme(darkTheme = false) {
        TransactionHistoryScreen(
            uiState =
                HistoryUIState(
                    isLoading = false,
                    items = previewHistoryItems(),
                ),
            onBack = {},
            onAddTransaction = {},
        )
    }
}

@Preview(
    name = "History — Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun TransactionHistoryScreenDarkPreview() {
    KeuTrackTheme(darkTheme = true) {
        TransactionHistoryScreen(
            uiState =
                HistoryUIState(
                    isLoading = false,
                    items = previewHistoryItems(),
                ),
            onBack = {},
            onAddTransaction = {},
        )
    }
}

@Preview(showBackground = true, name = "History — Empty")
@Composable
private fun TransactionHistoryEmptyPreview() {
    KeuTrackTheme {
        TransactionHistoryScreen(
            uiState = HistoryUIState(isLoading = false),
            onBack = {},
            onAddTransaction = {},
        )
    }
}

@Preview(showBackground = true, name = "History — Empty filtered")
@Composable
private fun TransactionHistoryEmptyFilteredPreview() {
    KeuTrackTheme {
        TransactionHistoryScreen(
            uiState =
                HistoryUIState(
                    isLoading = false,
                    periodPreset = HistoryPeriodPreset.Last7Days,
                    periodSummaryLabel = "7 hari",
                    hasActivePeriodFilter = true,
                ),
            onBack = {},
            onAddTransaction = {},
        )
    }
}

private fun historyTitle(scope: HistoryScope): String =
    when (scope) {
        HistoryScope.All -> HISTORY_TITLE
        HistoryScope.Personal -> HISTORY_PERSONAL_TITLE
        HistoryScope.Family -> HISTORY_FAMILY_TITLE
    }

private fun historyEmptyTitle(scope: HistoryScope): String =
    when (scope) {
        HistoryScope.All -> HISTORY_EMPTY_TITLE
        HistoryScope.Personal -> HISTORY_PERSONAL_EMPTY_TITLE
        HistoryScope.Family -> HISTORY_FAMILY_EMPTY_TITLE
    }

private fun historyEmptyBody(scope: HistoryScope): String =
    when (scope) {
        HistoryScope.All -> HISTORY_EMPTY_BODY
        HistoryScope.Personal -> HISTORY_PERSONAL_EMPTY_BODY
        HistoryScope.Family -> HISTORY_FAMILY_EMPTY_BODY
    }

private fun previewHistoryItems(): List<TransactionRowUi> =
    listOf(
        TransactionRowUi(
            id = "1",
            title = "Bakmi GM Restaurant",
            categoryLabel = "Food & Drinks",
            timeLabel = "12:45 PM",
            amountLabel = "IDR 125.000",
            isExpense = true,
            walletLabel = "Personal",
            categoryIcon = TransactionCategoryIcon.Restaurant,
            syncStatus = SyncStatus.PENDING,
        ),
        TransactionRowUi(
            id = "2",
            title = "Salary — March",
            categoryLabel = "Payout",
            timeLabel = "Mar 1",
            amountLabel = "IDR 5.500.000",
            isExpense = false,
            walletLabel = "Personal",
            categoryIcon = TransactionCategoryIcon.Payout,
        ),
    )
