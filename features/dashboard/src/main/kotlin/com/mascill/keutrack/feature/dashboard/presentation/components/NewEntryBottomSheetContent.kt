package com.mascill.keutrack.feature.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackAmountKeypad
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.component.KeuTrackCategoryChip
import com.mascill.keutrack.core.designsystem.component.KeuTrackCurrencyText
import com.mascill.keutrack.core.designsystem.component.KeuTrackSegmentedControl
import com.mascill.keutrack.core.designsystem.format.MAX_AMOUNT_RUPIAH
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.dashboard.presentation.model.EntryTransactionKind
import com.mascill.keutrack.feature.dashboard.presentation.model.NewEntryCategoryUI

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
private const val NEW_ENTRY_WALLET_CHIP_LABEL_GAP = 6
private const val NEW_ENTRY_WALLET_CHIP_PH = 12
private const val NEW_ENTRY_WALLET_CHIP_PV = 12
private const val NEW_ENTRY_WALLET_CHIP_ICON_TEXT_SPACING = 8
private const val NEW_ENTRY_WALLET_CHIP_ICON = 20

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

        KeuTrackSegmentedControl(
            leftLabel = NEW_ENTRY_EXPENSE,
            rightLabel = NEW_ENTRY_INCOME,
            leftSelected = kind == EntryTransactionKind.Expense,
            onLeftClick = { kind = EntryTransactionKind.Expense },
            onRightClick = { kind = EntryTransactionKind.Income },
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
            KeuTrackCurrencyText(
                amount = amountRupiah,
                style = typography.headingBold36,
                color = semantic.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
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
                KeuTrackCategoryChip(
                    label = cat.label,
                    icon = cat.icon,
                    containerColor = cat.accent,
                    selected = selectedCategoryId == cat.id,
                    onClick = { selectedCategoryId = cat.id },
                )
            }
        }

        Spacer(modifier = Modifier.height(NEW_ENTRY_BEFORE_KEYPAD_SPACER.dp))

        KeuTrackAmountKeypad(
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
