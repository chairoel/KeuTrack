package com.mascill.keutrack.feature.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val WALLET_FAMILY_CARD_CONTENT_PAD = 20
private const val WALLET_PERSONAL_PH = 20
private const val WALLET_PERSONAL_PV = 22
private const val WALLET_HEADER_TITLE_END_PAD = 8
private const val WALLET_HEADER_ICON_SIZE = 20
private const val WALLET_BALANCE_LABEL_PT = 16
private const val WALLET_BALANCE_AMOUNT_PT = 4
private const val WALLET_LABEL_ON_GRADIENT_ALPHA = 0.9f
private const val WALLET_HEADER_TITLE_ALPHA = 0.85f
private const val WALLET_AVATAR_SIZE = 28
private const val WALLET_AVATAR_OVERLAP = 14
private const val WALLET_AVATAR_FILL_ALPHA = 0.35f
private const val WALLET_AVATAR_BORDER_WIDTH = 1.5
private const val WALLET_MONTH_CHIP_PT = 12
private const val WALLET_MONTH_CHIP_SURFACE_ALPHA = 0.35f
private const val WALLET_MONTH_CHIP_PH = 10
private const val WALLET_MONTH_CHIP_PV = 6
private const val WALLET_FAMILY_FOOTER_ROW_PT = 14
private const val WALLET_FAMILY_FOOTER_AVATAR_END = 10

enum class WalletSummaryCardKind {
    Personal,
    Family,
}

/**
 * Shared wallet summary layout: header → balance label → amount → [footer].
 * [WalletSummaryCardKind.Personal] uses gradient hero styling; [WalletSummaryCardKind.Family] uses [KeuTrackCard].
 */
@Composable
fun WalletSummaryCard(
    kind: WalletSummaryCardKind,
    balanceLabel: String,
    balanceAmount: String,
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit,
) {
    when (kind) {
        WalletSummaryCardKind.Personal ->
            PersonalWalletSurface(modifier = modifier) {
                WalletSummaryCardBody(
                    kind = kind,
                    balanceLabel = balanceLabel,
                    balanceAmount = balanceAmount,
                    footer = footer,
                )
            }

        WalletSummaryCardKind.Family ->
            KeuTrackCard(
                modifier = modifier.fillMaxWidth(),
                highlighted = true,
                contentPadding = PaddingValues(WALLET_FAMILY_CARD_CONTENT_PAD.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    WalletSummaryCardBody(
                        kind = kind,
                        balanceLabel = balanceLabel,
                        balanceAmount = balanceAmount,
                        footer = footer,
                    )
                }
            }
    }
}

@Composable
private fun PersonalWalletSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val gradient =
        Brush.linearGradient(
            colors =
                listOf(
                    semantic.primaryGradientStart,
                    semantic.primaryGradientEnd,
                ),
        )
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(shapes.radiusXl))
                .background(brush = gradient)
                .padding(horizontal = WALLET_PERSONAL_PH.dp, vertical = WALLET_PERSONAL_PV.dp),
        content = content,
    )
}

@Composable
private fun ColumnScope.WalletSummaryCardBody(
    kind: WalletSummaryCardKind,
    balanceLabel: String,
    balanceAmount: String,
    footer: @Composable ColumnScope.() -> Unit,
) {
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography
    val neutral = KeuTrackTheme.neutralColors
    val textColors = KeuTrackTheme.textColors

    val (headerIcon, headerTitle, labelColor, amountColor) =
        when (kind) {
            WalletSummaryCardKind.Personal ->
                Quadruple(
                    first = Icons.Filled.AccountBalanceWallet,
                    second = "PERSONAL WALLET",
                    third = neutral.white.copy(alpha = WALLET_LABEL_ON_GRADIENT_ALPHA),
                    fourth = neutral.white,
                )

            WalletSummaryCardKind.Family ->
                Quadruple(
                    first = Icons.Filled.Groups,
                    second = "FAMILY WALLET",
                    third = textColors.body,
                    fourth = textColors.title,
                )
        }

    val headerIconTint =
        when (kind) {
            WalletSummaryCardKind.Personal -> neutral.white
            WalletSummaryCardKind.Family -> semantic.primary
        }
    val headerTitleColor =
        when (kind) {
            WalletSummaryCardKind.Personal -> neutral.white.copy(alpha = WALLET_HEADER_TITLE_ALPHA)
            WalletSummaryCardKind.Family -> textColors.body
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = headerTitle,
            style = typography.bodyBold10,
            color = headerTitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = WALLET_HEADER_TITLE_END_PAD.dp),
        )
        Icon(
            imageVector = headerIcon,
            contentDescription = null,
            tint = headerIconTint,
            modifier = Modifier.size(WALLET_HEADER_ICON_SIZE.dp),
        )
    }

    Text(
        text = balanceLabel,
        style = typography.bodyRegular14,
        color = labelColor,
        modifier = Modifier.padding(top = WALLET_BALANCE_LABEL_PT.dp),
    )

    Text(
        text = balanceAmount,
        style = typography.headingBold30,
        color = amountColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = WALLET_BALANCE_AMOUNT_PT.dp),
    )

    footer()
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

@Composable
private fun FamilyWalletAvatarStack(modifier: Modifier = Modifier) {
    val semantic = KeuTrackTheme.semanticColors
    val initials = listOf("A", "B", "C", "D")
    val accents =
        listOf(
            semantic.primary,
            semantic.secondary,
            semantic.tertiary,
            semantic.success,
        )
    val avatarSize = WALLET_AVATAR_SIZE.dp
    val overlap = WALLET_AVATAR_OVERLAP.dp

    Box(
        modifier =
            modifier
                .width(avatarSize + overlap * (initials.size - 1))
                .height(avatarSize),
    ) {
        initials.forEachIndexed { index, letter ->
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .zIndex(index.toFloat())
                        .offset(x = overlap * index)
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(
                            accents[index % accents.size].copy(alpha = WALLET_AVATAR_FILL_ALPHA),
                        )
                        .border(
                            width = WALLET_AVATAR_BORDER_WIDTH.dp,
                            color = semantic.surfaceContainerLowest,
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    style = KeuTrackTheme.typography.bodyBold10,
                    color = semantic.onSurface,
                )
            }
        }
    }
}

/** Default footer for personal wallet: month change chip. */
@Composable
fun WalletSummaryPersonalMonthChangeFooter(monthChangeLabel: String) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val typography = KeuTrackTheme.typography
    val success = KeuTrackTheme.successColors

    Box(
        modifier =
            Modifier
                .padding(top = WALLET_MONTH_CHIP_PT.dp)
                .clip(RoundedCornerShape(shapes.radiusMd))
                .background(
                    semantic.surfaceContainerHigh.copy(alpha = WALLET_MONTH_CHIP_SURFACE_ALPHA),
                )
                .padding(horizontal = WALLET_MONTH_CHIP_PH.dp, vertical = WALLET_MONTH_CHIP_PV.dp),
    ) {
        Text(
            text = monthChangeLabel,
            style = typography.bodyBold12,
            color = success.s300,
        )
    }
}

/** Default footer for family wallet: overlapping avatars + shared summary. */
@Composable
fun WalletSummaryFamilySharedFooter(sharedSummary: String) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors

    Row(
        modifier = Modifier.padding(top = WALLET_FAMILY_FOOTER_ROW_PT.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FamilyWalletAvatarStack(modifier = Modifier.padding(end = WALLET_FAMILY_FOOTER_AVATAR_END.dp))
        Text(
            text = sharedSummary,
            style = typography.bodyRegular12,
            color = textColors.body,
        )
    }
}

//private val PreviewWalletSamplePersonal =
//    Triple("Current Balance", "IDR 12.450.000", "+2.4% this month")
//
//private val PreviewWalletSampleFamily =
//    Triple("Available Shared", "IDR 45.820.500", "Shared with 4 people")
//
//@Preview(showBackground = true, name = "Wallet cards — Stacked")
//@Composable
//private fun WalletCardsStackedPreview() {
//    KeuTrackTheme(darkTheme = false) {
//        Column(
//            modifier =
//                Modifier
//                    .fillMaxWidth()
//                    .background(KeuTrackTheme.contentColors.pageColor)
//                    .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp),
//        ) {
//            WalletSummaryCard(
//                kind = WalletSummaryCardKind.Personal,
//                balanceLabel = PreviewWalletSamplePersonal.first,
//                balanceAmount = PreviewWalletSamplePersonal.second,
//            ) {
//                WalletSummaryPersonalMonthChangeFooter(PreviewWalletSamplePersonal.third)
//            }
//            WalletSummaryCard(
//                kind = WalletSummaryCardKind.Family,
//                balanceLabel = PreviewWalletSampleFamily.first,
//                balanceAmount = PreviewWalletSampleFamily.second,
//            ) {
//                WalletSummaryFamilySharedFooter(PreviewWalletSampleFamily.third)
//            }
//        }
//    }
//}
