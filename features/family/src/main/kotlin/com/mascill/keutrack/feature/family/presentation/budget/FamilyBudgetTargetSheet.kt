package com.mascill.keutrack.feature.family.presentation.budget

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.component.KeuTrackModalBottomSheet
import com.mascill.keutrack.core.designsystem.component.KeuTrackTextField
import com.mascill.keutrack.core.designsystem.format.CurrencyFormat
import com.mascill.keutrack.core.designsystem.model.KeuTrackButtonStyle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.family.presentation.model.FamilyBudgetCategoryOption
import com.mascill.keutrack.feature.family.presentation.model.FamilyBudgetSheetState

private const val SHEET_TITLE = "Atur Target"
private const val SHEET_MONTH_PREFIX = "Bulan: "
private const val SHEET_CATEGORY_LABEL = "Kategori"
private const val SHEET_CATEGORY_PLACEHOLDER = "Pilih kategori"
private const val SHEET_LIMIT_LABEL = "Limit (Rp)"
private const val SHEET_LIMIT_PLACEHOLDER = "0"
private const val SHEET_PREVIEW_PREFIX = "Preview: "
private const val SHEET_SAVE = "Simpan target"
private const val SHEET_DELETE = "Hapus target"
private const val SHEET_PH = 20
private const val SHEET_PB = 16
private const val SHEET_TITLE_PB = 4
private const val SHEET_SECTION_SPACING = 16

@Composable
fun FamilyBudgetTargetSheet(
    sheet: FamilyBudgetSheetState,
    monthLabel: String,
    categories: List<FamilyBudgetCategoryOption>,
    isSaving: Boolean,
    onCategorySelected: (String) -> Unit,
    onLimitChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    KeuTrackModalBottomSheet(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
    ) {
        FamilyBudgetTargetSheetContent(
            sheet = sheet,
            monthLabel = monthLabel,
            categories = categories,
            isSaving = isSaving,
            onCategorySelected = onCategorySelected,
            onLimitChanged = onLimitChanged,
            onSave = onSave,
            onDelete = onDelete,
        )
    }
}

@Composable
internal fun FamilyBudgetTargetSheetContent(
    sheet: FamilyBudgetSheetState,
    monthLabel: String,
    categories: List<FamilyBudgetCategoryOption>,
    isSaving: Boolean,
    onCategorySelected: (String) -> Unit,
    onLimitChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val semantic = KeuTrackTheme.semanticColors
    val selectedName = categories.firstOrNull { it.id == sheet.categoryId }?.name
    val limitAmount = sheet.limitInput.toLongOrNull() ?: 0L
    val canSave =
        !isSaving && !sheet.categoryId.isNullOrBlank() && limitAmount > 0L

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SHEET_PH.dp)
                .padding(bottom = SHEET_PB.dp)
                .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(SHEET_SECTION_SPACING.dp),
    ) {
        Column {
            Text(
                text = SHEET_TITLE,
                style = typography.headingBold20,
                color = textColors.title,
                modifier = Modifier.padding(bottom = SHEET_TITLE_PB.dp),
            )
            Text(
                text = SHEET_MONTH_PREFIX + monthLabel,
                style = typography.bodyRegular14,
                color = semantic.onSurfaceVariant,
            )
        }

        CategoryPicker(
            selectedName = selectedName,
            categories = categories,
            locked = sheet.categoryLocked,
            enabled = !isSaving,
            onCategorySelected = onCategorySelected,
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            KeuTrackTextField(
                value = sheet.limitInput,
                onValueChange = onLimitChanged,
                label = SHEET_LIMIT_LABEL,
                placeholder = SHEET_LIMIT_PLACEHOLDER,
                isError = sheet.errorMessage != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Text(
                text = SHEET_PREVIEW_PREFIX + CurrencyFormat.formatIdr(limitAmount),
                style = typography.bodyRegular12,
                color = semantic.onSurfaceVariant,
            )
            sheet.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = typography.bodyRegular12,
                    color = semantic.error,
                )
            }
        }

        KeuTrackButton(
            text = SHEET_SAVE,
            onClick = onSave,
            enabled = canSave,
            isLoading = isSaving,
            style = KeuTrackButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth(),
        )

        if (sheet.existingBudgetId != null) {
            TextButton(
                onClick = onDelete,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = SHEET_DELETE,
                    style = typography.bodyBold16,
                    color = semantic.error,
                )
            }
        }
    }
}

@Composable
private fun CategoryPicker(
    selectedName: String?,
    categories: List<FamilyBudgetCategoryOption>,
    locked: Boolean,
    enabled: Boolean,
    onCategorySelected: (String) -> Unit,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val semantic = KeuTrackTheme.semanticColors
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = SHEET_CATEGORY_LABEL,
            style = typography.bodyRegular12,
            color = semantic.onSurfaceVariant,
        )
        if (locked) {
            Text(
                text = selectedName ?: SHEET_CATEGORY_PLACEHOLDER,
                style = typography.bodyBold16,
                color = textColors.title,
            )
        } else {
            Box {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { expanded = true }
                            .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = selectedName ?: SHEET_CATEGORY_PLACEHOLDER,
                        style = typography.bodyBold16,
                        color = if (selectedName == null) {
                            semantic.onSurfaceVariant
                        } else {
                            textColors.title
                        },
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = SHEET_CATEGORY_LABEL,
                        tint = semantic.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    categories.forEach { option ->
                        DropdownMenuItem(
                            onClick = {
                                onCategorySelected(option.id)
                                expanded = false
                            },
                        ) {
                            Text(text = option.name, style = typography.bodyRegular14)
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FamilyBudgetTargetSheetPreview() {
    KeuTrackTheme {
        FamilyBudgetTargetSheetContent(
            sheet =
                FamilyBudgetSheetState(
                    categoryId = "cat_food",
                    categoryLocked = false,
                    limitInput = "2000000",
                    existingBudgetId = "b-1",
                ),
            monthLabel = "Agustus 2026",
            categories =
                listOf(
                    FamilyBudgetCategoryOption("cat_food", "Makanan"),
                    FamilyBudgetCategoryOption("cat_school", "Pendidikan"),
                ),
            isSaving = false,
            onCategorySelected = {},
            onLimitChanged = {},
            onSave = {},
            onDelete = {},
        )
    }
}

@Preview(name = "Locked row — Light", showBackground = true)
@Preview(
    name = "Locked row — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun FamilyBudgetTargetSheetLockedPreview() {
    KeuTrackTheme {
        FamilyBudgetTargetSheetContent(
            sheet =
                FamilyBudgetSheetState(
                    categoryId = "cat_food",
                    categoryLocked = true,
                    limitInput = "1000000",
                    existingBudgetId = null,
                    errorMessage = "Limit harus lebih dari 0",
                ),
            monthLabel = "Agustus 2026",
            categories = listOf(FamilyBudgetCategoryOption("cat_food", "Makanan")),
            isSaving = false,
            onCategorySelected = {},
            onLimitChanged = {},
            onSave = {},
            onDelete = {},
        )
    }
}
