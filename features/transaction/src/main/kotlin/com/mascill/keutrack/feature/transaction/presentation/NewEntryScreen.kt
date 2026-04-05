package com.mascill.keutrack.feature.transaction.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

@Composable
fun NewEntryScreen(
    onBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "New Transaction",
            style = KeuTrackTheme.typography.bodyRegular16,
            color = KeuTrackTheme.textColors.body,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NewEntryScreenPreview() {
    KeuTrackTheme {
        NewEntryScreen()
    }
}
