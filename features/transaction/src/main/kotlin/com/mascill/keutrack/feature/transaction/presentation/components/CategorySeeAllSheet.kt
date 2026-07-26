package com.mascill.keutrack.feature.transaction.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackCategoryChip
import com.mascill.keutrack.core.designsystem.component.KeuTrackModalBottomSheet
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.transaction.presentation.model.NewEntryCategoryUI

private const val CATEGORY_SEE_ALL_TITLE = "Semua Kategori"
private const val CATEGORY_SEE_ALL_PH = 20
private const val CATEGORY_SEE_ALL_PB = 16
private const val CATEGORY_SEE_ALL_TITLE_PB = 16
private const val CATEGORY_SEE_ALL_GRID_MIN = 100
private const val CATEGORY_SEE_ALL_SPACING = 12

@Composable
fun CategorySeeAllSheet(
    categories: List<NewEntryCategoryUI>,
    selectedCategoryId: String?,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors

    KeuTrackModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CATEGORY_SEE_ALL_PH.dp)
                    .padding(bottom = CATEGORY_SEE_ALL_PB.dp)
                    .navigationBarsPadding(),
        ) {
            Text(
                text = CATEGORY_SEE_ALL_TITLE,
                style = typography.headingBold20,
                color = textColors.title,
                modifier = Modifier.padding(bottom = CATEGORY_SEE_ALL_TITLE_PB.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = CATEGORY_SEE_ALL_GRID_MIN.dp),
                horizontalArrangement = Arrangement.spacedBy(CATEGORY_SEE_ALL_SPACING.dp),
                verticalArrangement = Arrangement.spacedBy(CATEGORY_SEE_ALL_SPACING.dp),
            ) {
                items(categories, key = { it.id }) { cat ->
                    KeuTrackCategoryChip(
                        label = cat.label,
                        icon = cat.icon,
                        containerColor = cat.accent,
                        selected = selectedCategoryId == cat.id,
                        onClick = {
                            onCategorySelected(cat.id)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}
