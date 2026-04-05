package com.mascill.keutrack.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.model.KeuTrackBottomNavItem
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val NAV_ALPHA = 0.8f
private const val NAV_PH = 12
private const val NAV_PV = 8

private const val NAV_ITEM_ALPHA = 0.16f
private const val NAV_ITEM_PH = 14
private const val NAV_ITEM_PV = 8

private const val NAV_ITEM_RIPPLE_RADIUS = 80
private const val NAV_ITEM_RIPPLE_BOUNDED = true

private const val NAV_ITEM_ICON_SIZE = 18
private const val NAV_ITEM_TEXT_PS = 6

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
                color = semantic.surfaceContainerHighest.copy(alpha = NAV_ALPHA),
                shape = RoundedCornerShape(shapes.radiusXl)
            )
            .padding(PaddingValues(horizontal = NAV_PH.dp, vertical = NAV_PV.dp)),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val interactionSource = remember { MutableInteractionSource() }

            val selected = item.key == selectedKey
            val iconColor = if (selected) {
                semantic.primary
            } else {
                semantic.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(shapes.radiusLg))
                    .background(
                        color = if (selected) {
                            semantic.primary.copy(alpha = NAV_ITEM_ALPHA)
                        } else {
                            Color.Transparent
                        }, shape = RoundedCornerShape(shapes.radiusLg)
                    )
                    .indication(
                        interactionSource = interactionSource,
                        indication = ripple(
                            bounded = NAV_ITEM_RIPPLE_BOUNDED,
                            radius = NAV_ITEM_RIPPLE_RADIUS.dp
                        )
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onItemClick(item.key) }
                    .padding(horizontal = NAV_ITEM_PH.dp, vertical = NAV_ITEM_PV.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconColor,
                        modifier = Modifier.size(NAV_ITEM_ICON_SIZE.dp)
                    )
                    if (selected) {
                        Text(
                            text = item.label,
                            color = iconColor,
                            style = KeuTrackTheme.typography.bodyBold12,
                            modifier = Modifier.padding(start = NAV_ITEM_TEXT_PS.dp)
                        )
                    }
                }
            }
        }
    }
}
