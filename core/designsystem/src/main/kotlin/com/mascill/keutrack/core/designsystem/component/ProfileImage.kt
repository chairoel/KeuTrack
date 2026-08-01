package com.mascill.keutrack.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val PROFILE_AVATAR = 56
private const val PROFILE_AVATAR_ICON = 44

@Composable
fun ProfileImage(
    photoUrl: String?,
    avatarSize: Int = PROFILE_AVATAR,
    iconSize: Int = PROFILE_AVATAR_ICON,
) {
    val semantic = KeuTrackTheme.semanticColors

    Box(
        modifier = Modifier
            .size(avatarSize.dp)
            .clip(CircleShape)
            .background(semantic.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = null,
                tint = semantic.onSurfaceVariant,
                modifier = Modifier.size(iconSize.dp),
            )
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ProfileImagePreview() {
    KeuTrackTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ProfileImage(photoUrl = null)
        }
    }
}