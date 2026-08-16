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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.component.KeuTrackTopBar
import com.mascill.keutrack.core.designsystem.component.snackbar.KeuTrackInlineSnackbar
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.feature.transaction.presentation.components.TransactionHistoryRow
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryUIState
import com.mascill.keutrack.feature.transaction.presentation.model.TransactionCategoryIcon
import com.mascill.keutrack.feature.transaction.presentation.model.TransactionRowUi

private const val HISTORY_TITLE = "Riwayat"
private const val HISTORY_FAMILY_TITLE = "Riwayat Keluarga"
private const val HISTORY_EMPTY_TITLE = "Belum ada transaksi"
private const val HISTORY_EMPTY_BODY = "Catat pemasukan atau pengeluaran pertamamu."
private const val HISTORY_FAMILY_EMPTY_TITLE = "Belum ada transaksi keluarga"
private const val HISTORY_FAMILY_EMPTY_BODY =
    "Belum ada transaksi di dompet keluarga. Gunakan tombol di bawah untuk menambah transaksi bersama."
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

@Composable
fun TransactionHistoryScreen(
    uiState: HistoryUIState,
    onBack: () -> Unit,
    onAddTransaction: () -> Unit,
    onDismissError: () -> Unit = {},
) {
    val pageBg = KeuTrackTheme.contentColors.pageColor
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors

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
                        title = if (uiState.isFamilyOnly) HISTORY_FAMILY_TITLE else HISTORY_TITLE,
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
            when {
                uiState.isLoading -> {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = semantic.primary)
                    }
                }

                uiState.items.isEmpty() -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = HISTORY_CONTENT_PH.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (uiState.isFamilyOnly) {
                                HISTORY_FAMILY_EMPTY_TITLE
                            } else {
                                HISTORY_EMPTY_TITLE
                            },
                            style = typography.headingBold20,
                            color = textColors.title,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = if (uiState.isFamilyOnly) {
                                HISTORY_FAMILY_EMPTY_BODY
                            } else {
                                HISTORY_EMPTY_BODY
                            },
                            style = typography.bodyRegular14,
                            color = textColors.body,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = HISTORY_EMPTY_SPACING.dp),
                        )
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

                else -> {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
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

        uiState.errorMessage?.let { message ->
            KeuTrackInlineSnackbar(
                message = message,
                onDismiss = onDismissError,
                actionLabel = HISTORY_ERROR_DISMISS,
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
