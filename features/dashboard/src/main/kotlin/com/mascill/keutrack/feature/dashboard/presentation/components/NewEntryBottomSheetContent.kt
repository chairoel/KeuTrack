package com.mascill.keutrack.feature.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.dashboard.presentation.model.EntryTransactionKind
import com.mascill.keutrack.feature.dashboard.presentation.model.NewEntryCategoryUI
import com.mascill.keutrack.feature.dashboard.presentation.model.NewEntryKeypad
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

/** Max whole rupiah digits the keypad can represent (≈100 trillion). */
private const val MAX_AMOUNT_RUPIAH = 99_999_999_999_999L

private const val NEW_ENTRY_TITLE = "New Entry"
private const val NEW_ENTRY_SUBTITLE = "Add a transaction to your ledger"
private const val NEW_ENTRY_AMOUNT_SECTION = "AMOUNT"
private const val NEW_ENTRY_WALLET_TYPE_LABEL = "WALLET TYPE"
private const val NEW_ENTRY_WALLET_TYPE_VALUE = "Family"
private const val NEW_ENTRY_DATE_LABEL = "DATE"
private const val NEW_ENTRY_DATE_VALUE = "Today"
private const val NEW_ENTRY_CATEGORY_SECTION = "CATEGORY"
private const val NEW_ENTRY_SEE_ALL = "See all"
private const val NEW_ENTRY_ADD_TRANSACTION = "Add transaction"
private const val NEW_ENTRY_EXPENSE = "Expense"
private const val NEW_ENTRY_INCOME = "Income"
private const val NEW_ENTRY_CATEGORY_ID_FOOD = "food"
private const val NEW_ENTRY_CATEGORY_LABEL_FOOD = "Food"
private const val NEW_ENTRY_CATEGORY_ID_TRANSPORT = "transport"
private const val NEW_ENTRY_CATEGORY_LABEL_TRANSPORT = "Transport"
private const val NEW_ENTRY_CATEGORY_ID_BILLS = "bills"
private const val NEW_ENTRY_CATEGORY_LABEL_BILLS = "Bills"
private const val NEW_ENTRY_CATEGORY_ID_FUN = "fun"
private const val NEW_ENTRY_CATEGORY_LABEL_FUN = "Fun"
private const val NEW_ENTRY_AMOUNT_CURRENCY_PREFIX = "Rp "

private const val NEW_ENTRY_SHEET_PH = 20
private const val NEW_ENTRY_SHEET_PB = 8
private const val NEW_ENTRY_SUBTITLE_PT = 4
private const val NEW_ENTRY_SECTION_SPACER_LG = 20
private const val NEW_ENTRY_AMOUNT_LABEL_PB = 8
private const val NEW_ENTRY_AMOUNT_CARD_PV = 20
private const val NEW_ENTRY_AMOUNT_CARD_PH = 16
private const val NEW_ENTRY_AFTER_AMOUNT_CARD_SPACER = 16
private const val NEW_ENTRY_WALLET_ROW_SPACING = 12
private const val NEW_ENTRY_CATEGORY_HEADER_PB = 12
private const val NEW_ENTRY_CATEGORY_ROW_SPACING = 12
private const val NEW_ENTRY_BEFORE_KEYPAD_SPACER = 20
private const val NEW_ENTRY_AFTER_KEYPAD_SPACER = 20
private const val NEW_ENTRY_TOGGLE_PADDING = 4
private const val NEW_ENTRY_TOGGLE_INNER_SPACING = 4
private const val NEW_ENTRY_TOGGLE_SEGMENT_PV = 12
private const val NEW_ENTRY_WALLET_CHIP_LABEL_GAP = 6
private const val NEW_ENTRY_WALLET_CHIP_PH = 12
private const val NEW_ENTRY_WALLET_CHIP_PV = 12
private const val NEW_ENTRY_WALLET_CHIP_ICON_TEXT_SPACING = 8
private const val NEW_ENTRY_WALLET_CHIP_ICON = 20
private const val NEW_ENTRY_CATEGORY_CHIP_BOX = 56
private const val NEW_ENTRY_CATEGORY_CHIP_ICON = 26
private const val NEW_ENTRY_CATEGORY_CHIP_LABEL_PT = 6
private const val NEW_ENTRY_KEYPAD_ROW_SPACING = 8
private const val NEW_ENTRY_KEYPAD_CELL_HEIGHT = 52

private const val NEW_ENTRY_TOGGLE_EXPENSE_PRIMARY_CONTAINER_ALPHA = 0.85f
private const val NEW_ENTRY_TOGGLE_INCOME_PRIMARY_LIGHT_ALPHA = 0.1f
private const val NEW_ENTRY_TOGGLE_INCOME_PRIMARY_DARK_ALPHA = 0.35f
private const val NEW_ENTRY_CATEGORY_CHIP_SELECTED_ALPHA = 0.85f

private val newEntryIdNumberFormat: NumberFormat =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
        isGroupingUsed = true
    }

@Composable
fun NewEntryBottomSheetContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val textColors = KeuTrackTheme.textColors
    val typography = KeuTrackTheme.typography

    var kind by remember { mutableStateOf(EntryTransactionKind.Expense) }
    var amountRupiah by remember { mutableLongStateOf(0L) }
    var selectedCategoryId by remember { mutableStateOf(NEW_ENTRY_CATEGORY_ID_FOOD) }

    val categories =
        listOf(
            NewEntryCategoryUI(
                NEW_ENTRY_CATEGORY_ID_FOOD,
                NEW_ENTRY_CATEGORY_LABEL_FOOD,
                Icons.Outlined.Restaurant,
                KeuTrackTheme.warningColors.w500,
            ),
            NewEntryCategoryUI(
                NEW_ENTRY_CATEGORY_ID_TRANSPORT,
                NEW_ENTRY_CATEGORY_LABEL_TRANSPORT,
                Icons.Outlined.DirectionsCar,
                semantic.primary,
            ),
            NewEntryCategoryUI(
                NEW_ENTRY_CATEGORY_ID_BILLS,
                NEW_ENTRY_CATEGORY_LABEL_BILLS,
                Icons.Outlined.Receipt,
                textColors.link,
            ),
            NewEntryCategoryUI(
                NEW_ENTRY_CATEGORY_ID_FUN,
                NEW_ENTRY_CATEGORY_LABEL_FUN,
                Icons.Outlined.Movie,
                KeuTrackTheme.successColors.s500,
            ),
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NEW_ENTRY_SHEET_PH.dp)
                .padding(bottom = NEW_ENTRY_SHEET_PB.dp),
    ) {
        Text(
            text = NEW_ENTRY_TITLE,
            style = typography.headingBold24,
            color = textColors.title,
        )
        Text(
            text = NEW_ENTRY_SUBTITLE,
            style = typography.bodyRegular14,
            color = textColors.body,
            modifier = Modifier.padding(top = NEW_ENTRY_SUBTITLE_PT.dp),
        )

        Spacer(modifier = Modifier.height(NEW_ENTRY_SECTION_SPACER_LG.dp))

        ExpenseIncomeToggle(
            selected = kind,
            onSelect = { kind = it },
        )

        Spacer(modifier = Modifier.height(NEW_ENTRY_SECTION_SPACER_LG.dp))

        Text(
            text = NEW_ENTRY_AMOUNT_SECTION,
            style = typography.bodyBold10,
            color = textColors.body,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            letterSpacing = typography.bodyBold10.letterSpacing,
        )
        Spacer(modifier = Modifier.height(NEW_ENTRY_AMOUNT_LABEL_PB.dp))

        KeuTrackCard(
            modifier = Modifier.fillMaxWidth(),
            highlighted = true,
            contentPadding =
                PaddingValues(
                    vertical = NEW_ENTRY_AMOUNT_CARD_PV.dp,
                    horizontal = NEW_ENTRY_AMOUNT_CARD_PH.dp,
                ),
            onClick = null,
        ) {
            Text(
                text = formatAmountIdr(amountRupiah),
                style = typography.headingBold36,
                color = semantic.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(NEW_ENTRY_AFTER_AMOUNT_CARD_SPACER.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NEW_ENTRY_WALLET_ROW_SPACING.dp),
        ) {
            WalletDateChip(
                modifier = Modifier.weight(1f),
                label = NEW_ENTRY_WALLET_TYPE_LABEL,
                icon = Icons.Outlined.Group,
                value = NEW_ENTRY_WALLET_TYPE_VALUE,
                showTrailingChevron = true,
            )
            WalletDateChip(
                modifier = Modifier.weight(1f),
                label = NEW_ENTRY_DATE_LABEL,
                icon = Icons.Outlined.CalendarToday,
                value = NEW_ENTRY_DATE_VALUE,
                showTrailingChevron = false,
            )
        }

        Spacer(modifier = Modifier.height(NEW_ENTRY_SECTION_SPACER_LG.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = NEW_ENTRY_CATEGORY_SECTION,
                style = typography.bodyBold10,
                color = textColors.body,
            )
            Text(
                text = NEW_ENTRY_SEE_ALL,
                style = typography.bodyBold14,
                color = textColors.link,
                modifier = Modifier.clickable { },
            )
        }

        Spacer(modifier = Modifier.height(NEW_ENTRY_CATEGORY_HEADER_PB.dp))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(NEW_ENTRY_CATEGORY_ROW_SPACING.dp),
        ) {
            categories.forEach { cat ->
                CategoryChip(
                    label = cat.label,
                    icon = cat.icon,
                    accent = cat.accent,
                    selected = selectedCategoryId == cat.id,
                    onClick = { selectedCategoryId = cat.id },
                )
            }
        }

        Spacer(modifier = Modifier.height(NEW_ENTRY_BEFORE_KEYPAD_SPACER.dp))

        AmountKeypad(
            onDigit = { d ->
                val next = amountRupiah * 10L + d
                if (next <= MAX_AMOUNT_RUPIAH) amountRupiah = next
            },
            onTripleZero = {
                if (amountRupiah <= MAX_AMOUNT_RUPIAH / 1000L) {
                    amountRupiah *= 1000L
                }
            },
            onBackspace = { amountRupiah /= 10L },
        )

        Spacer(modifier = Modifier.height(NEW_ENTRY_AFTER_KEYPAD_SPACER.dp))

        KeuTrackButton(
            text = NEW_ENTRY_ADD_TRANSACTION,
            onClick = onDismiss,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
        )
    }
}

@Composable
private fun ExpenseIncomeToggle(
    selected: EntryTransactionKind,
    onSelect: (EntryTransactionKind) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val typography = KeuTrackTheme.typography

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(shapes.radiusLg))
                .background(semantic.surfaceContainerHigh)
                .padding(NEW_ENTRY_TOGGLE_PADDING.dp),
        horizontalArrangement = Arrangement.spacedBy(NEW_ENTRY_TOGGLE_INNER_SPACING.dp),
    ) {
        EntryTransactionKind.entries.forEach { entryKind ->
            val isSelected = selected == entryKind
            val bg =
                when {
                    !isSelected -> Color.Transparent
                    entryKind == EntryTransactionKind.Expense && !isDark ->
                        semantic.surfaceContainerLowest

                    entryKind == EntryTransactionKind.Expense && isDark ->
                        semantic.primaryContainer.copy(alpha = NEW_ENTRY_TOGGLE_EXPENSE_PRIMARY_CONTAINER_ALPHA)

                    entryKind == EntryTransactionKind.Income && !isDark ->
                        semantic.primary.copy(alpha = NEW_ENTRY_TOGGLE_INCOME_PRIMARY_LIGHT_ALPHA)

                    else -> semantic.primary.copy(alpha = NEW_ENTRY_TOGGLE_INCOME_PRIMARY_DARK_ALPHA)
                }
            val labelColor =
                when {
                    !isSelected -> semantic.onSurfaceVariant
                    entryKind == EntryTransactionKind.Expense && !isDark -> semantic.tertiary
                    entryKind == EntryTransactionKind.Expense && isDark -> Color.White
                    entryKind == EntryTransactionKind.Income && !isDark -> semantic.primary
                    else -> Color.White
                }
            val label =
                when (entryKind) {
                    EntryTransactionKind.Expense -> NEW_ENTRY_EXPENSE
                    EntryTransactionKind.Income -> NEW_ENTRY_INCOME
                }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(shapes.radiusMd))
                        .background(bg)
                        .clickable { onSelect(entryKind) }
                        .padding(vertical = NEW_ENTRY_TOGGLE_SEGMENT_PV.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = typography.bodyBold14,
                    color = labelColor,
                )
            }
        }
    }
}

@Composable
private fun WalletDateChip(
    label: String,
    icon: ImageVector,
    value: String,
    showTrailingChevron: Boolean,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val textColors = KeuTrackTheme.textColors
    val typography = KeuTrackTheme.typography
    val shapes = KeuTrackTheme.shapeTokens

    Column(modifier = modifier) {
        Text(
            text = label,
            style = typography.bodyBold10,
            color = textColors.body,
        )
        Spacer(modifier = Modifier.height(NEW_ENTRY_WALLET_CHIP_LABEL_GAP.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(shapes.radiusMd))
                    .background(semantic.surfaceContainerLow)
                    .clickable { }
                    .padding(
                        horizontal = NEW_ENTRY_WALLET_CHIP_PH.dp,
                        vertical = NEW_ENTRY_WALLET_CHIP_PV.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NEW_ENTRY_WALLET_CHIP_ICON_TEXT_SPACING.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = semantic.onSurfaceVariant,
                modifier = Modifier.size(NEW_ENTRY_WALLET_CHIP_ICON.dp),
            )
            Text(
                text = value,
                style = typography.bodyBold14,
                color = textColors.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showTrailingChevron) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = semantic.onSurfaceVariant,
                    modifier = Modifier.size(NEW_ENTRY_WALLET_CHIP_ICON.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val semantic = KeuTrackTheme.semanticColors
    val textColors = KeuTrackTheme.textColors
    val typography = KeuTrackTheme.typography
    val shapes = KeuTrackTheme.shapeTokens
    val chipShape = RoundedCornerShape(shapes.radiusMd)
    val chipInteractionSource = remember { MutableInteractionSource() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(NEW_ENTRY_CATEGORY_CHIP_BOX.dp)
                    .clip(chipShape)
                    .background(
                        if (selected) {
                            accent.copy(alpha = NEW_ENTRY_CATEGORY_CHIP_SELECTED_ALPHA)
                        } else {
                            semantic.surfaceContainerHigh
                        },
                    )
                    .clickable(
                        interactionSource = chipInteractionSource,
                        indication = ripple(bounded = true),
                        onClick = onClick,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint =
                    if (selected) {
                        Color.White
                    } else {
                        accent
                    },
                modifier = Modifier.size(NEW_ENTRY_CATEGORY_CHIP_ICON.dp),
            )
        }
        Spacer(modifier = Modifier.height(NEW_ENTRY_CATEGORY_CHIP_LABEL_PT.dp))
        Text(
            text = label,
            style = typography.bodyRegular12,
            color = textColors.body,
            maxLines = 1,
        )
    }
}

@Composable
private fun AmountKeypad(
    onDigit: (Long) -> Unit,
    onTripleZero: () -> Unit,
    onBackspace: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NEW_ENTRY_KEYPAD_ROW_SPACING.dp)) {
        NewEntryKeypad.ROWS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NEW_ENTRY_KEYPAD_ROW_SPACING.dp),
            ) {
                row.forEach { key ->
                    KeypadCell(
                        label = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (key) {
                                NewEntryKeypad.BACKSPACE -> onBackspace()
                                NewEntryKeypad.TRIPLE_ZERO -> onTripleZero()
                                else -> onDigit(key.first().digitToInt().toLong())
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadCell(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val textColors = KeuTrackTheme.textColors
    val typography = KeuTrackTheme.typography
    val shapes = KeuTrackTheme.shapeTokens

    Box(
        modifier =
            modifier
                .height(NEW_ENTRY_KEYPAD_CELL_HEIGHT.dp)
                .clip(RoundedCornerShape(shapes.radiusMd))
                .background(semantic.surfaceContainerHigh)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = typography.headingBold20,
            color = textColors.title,
        )
    }
}

private fun formatAmountIdr(amountRupiah: Long): String {
    val safe = min(amountRupiah, MAX_AMOUNT_RUPIAH)
    return NEW_ENTRY_AMOUNT_CURRENCY_PREFIX + newEntryIdNumberFormat.format(safe)
}
