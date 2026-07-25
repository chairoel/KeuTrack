package com.mascill.keutrack.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.format.CurrencyFormat
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

@Composable
fun KeuTrackCurrencyText(
    amount: Long,
    modifier: Modifier = Modifier,
    signed: Boolean = false,
    isExpense: Boolean? = null,
    style: TextStyle = KeuTrackTheme.typography.bodyBold16,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val textColors = KeuTrackTheme.textColors
    val success = KeuTrackTheme.successColors
    val danger = KeuTrackTheme.dangerColors

    val resolvedColor =
        when {
            color != Color.Unspecified -> color
            signed && isExpense == true -> danger.d500
            signed && isExpense == false -> success.s500
            else -> textColors.title
        }

    val text =
        if (signed && isExpense != null) {
            CurrencyFormat.formatIdrSigned(amount, isExpense)
        } else {
            CurrencyFormat.formatIdr(amount)
        }

    Text(
        text = text,
        style = style,
        color = resolvedColor,
        modifier = modifier,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun KeuTrackCurrencyTextPreview() {
    KeuTrackTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeuTrackCurrencyText(amount = 12_500L)
            KeuTrackCurrencyText(amount = 125_000L, signed = true, isExpense = true)
            KeuTrackCurrencyText(amount = 5_500_000L, signed = true, isExpense = false)
            KeuTrackCurrencyText(amount = 0L)
        }
    }
}
