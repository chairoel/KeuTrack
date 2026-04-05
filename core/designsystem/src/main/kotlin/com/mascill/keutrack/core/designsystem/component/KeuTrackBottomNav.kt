package com.mascill.keutrack.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

data class KeuTrackBottomNavItem(
    val key: String,
    val icon: ImageVector,
    val label: String,
)

@Composable
fun KeuTrackBottomNav(
    items: List<KeuTrackBottomNavItem>,
    selectedKey: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = semantic.surfaceContainerHighest.copy(alpha = 0.8f),
                shape = RoundedCornerShape(shapes.radiusXl)
            )
            .padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp)),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = item.key == selectedKey
            val iconColor = if (selected) semantic.primary else semantic.onSurfaceVariant

            Box(
                modifier = Modifier
                    .background(
                        color = if (selected) semantic.primary.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(shapes.radiusLg)
                    )
                    .clickable { onItemClick(item.key) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                    if (selected) {
                        Text(
                            text = item.label,
                            color = iconColor,
                            style = KeuTrackTheme.typography.bodyBold12,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
