package com.mascill.keutrack.feature.transaction.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackTopBar
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.transaction.presentation.components.CategorySeeAllSheet
import com.mascill.keutrack.feature.transaction.presentation.components.DatePickerDialogHost
import com.mascill.keutrack.feature.transaction.presentation.components.NewEntryFormContent
import com.mascill.keutrack.feature.transaction.presentation.components.WalletPickerBottomSheet
import com.mascill.keutrack.feature.transaction.presentation.model.EntryTransactionKind
import com.mascill.keutrack.feature.transaction.presentation.model.NewEntryCategoryUI
import com.mascill.keutrack.feature.transaction.presentation.model.NewEntryUIState
import com.mascill.keutrack.feature.transaction.presentation.model.TransactionUiMapper
import com.mascill.keutrack.feature.transaction.presentation.model.WalletOptionUi
import java.time.Instant
import java.time.LocalDate

private const val NEW_ENTRY_TITLE = "Transaksi Baru"
private const val NEW_ENTRY_TOP_BAR_ELEVATION = 4
private const val NEW_ENTRY_TOP_BAR_PH = 8
private const val NEW_ENTRY_TOP_BAR_PV = 4

@Composable
fun NewEntryScreen(
    uiState: NewEntryUIState,
    onBack: () -> Unit,
    onKindChanged: (EntryTransactionKind) -> Unit,
    onDigit: (Long) -> Unit,
    onTripleZero: () -> Unit,
    onBackspace: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onWalletSelected: (String) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSave: () -> Unit,
    onClearError: () -> Unit,
) {
    val pageBg = KeuTrackTheme.contentColors.pageColor
    val semantic = KeuTrackTheme.semanticColors

    var showWalletPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySeeAll by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = pageBg,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = pageBg,
                elevation = NEW_ENTRY_TOP_BAR_ELEVATION.dp,
            ) {
                KeuTrackTopBar(
                    title = NEW_ENTRY_TITLE,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(
                                horizontal = NEW_ENTRY_TOP_BAR_PH.dp,
                                vertical = NEW_ENTRY_TOP_BAR_PV.dp,
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

            else -> {
                NewEntryFormContent(
                    uiState = uiState,
                    onKindChanged = onKindChanged,
                    onDigit = onDigit,
                    onTripleZero = onTripleZero,
                    onBackspace = onBackspace,
                    onCategorySelected = onCategorySelected,
                    onWalletChipClick = { showWalletPicker = true },
                    onDateChipClick = { showDatePicker = true },
                    onSeeAllCategories = { showCategorySeeAll = true },
                    onNoteChanged = onNoteChanged,
                    onSave = onSave,
                    onClearError = onClearError,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }

    if (showWalletPicker) {
        WalletPickerBottomSheet(
            wallets = uiState.wallets,
            selectedWalletId = uiState.selectedWalletId,
            onDismiss = { showWalletPicker = false },
            onWalletSelected = onWalletSelected,
        )
    }

    if (showCategorySeeAll) {
        CategorySeeAllSheet(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            onDismiss = { showCategorySeeAll = false },
            onCategorySelected = onCategorySelected,
        )
    }

    DatePickerDialogHost(
        visible = showDatePicker,
        selectedDate = TransactionUiMapper.instantToLocalDate(uiState.selectedDate),
        onDateSelected = onDateSelected,
        onDismiss = { showDatePicker = false },
    )
}

@Preview(showBackground = true)
@Composable
private fun NewEntryScreenPreview() {
    KeuTrackTheme(darkTheme = false) {
        NewEntryScreen(
            uiState = previewNewEntryState(),
            onBack = {},
            onKindChanged = {},
            onDigit = {},
            onTripleZero = {},
            onBackspace = {},
            onCategorySelected = {},
            onWalletSelected = {},
            onDateSelected = {},
            onNoteChanged = {},
            onSave = {},
            onClearError = {},
        )
    }
}

@Preview(
    name = "New Entry — Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun NewEntryScreenDarkPreview() {
    KeuTrackTheme(darkTheme = true) {
        NewEntryScreen(
            uiState = previewNewEntryState(),
            onBack = {},
            onKindChanged = {},
            onDigit = {},
            onTripleZero = {},
            onBackspace = {},
            onCategorySelected = {},
            onWalletSelected = {},
            onDateSelected = {},
            onNoteChanged = {},
            onSave = {},
            onClearError = {},
        )
    }
}

private fun previewNewEntryState(): NewEntryUIState =
    NewEntryUIState(
        isLoading = false,
        amount = 125_000L,
        categories =
            listOf(
                NewEntryCategoryUI(
                    id = "food",
                    label = "Food",
                    icon = Icons.Outlined.Restaurant,
                    accent = Color(0xFFFF8A65),
                ),
            ),
        selectedCategoryId = "food",
        wallets =
            listOf(
                WalletOptionUi(
                    id = "w1",
                    name = "Personal",
                    typeLabel = "Personal",
                    familyId = null,
                ),
            ),
        selectedWalletId = "w1",
        selectedDate = Instant.now(),
        userId = "u1",
        addedByName = "Adhi",
    )
