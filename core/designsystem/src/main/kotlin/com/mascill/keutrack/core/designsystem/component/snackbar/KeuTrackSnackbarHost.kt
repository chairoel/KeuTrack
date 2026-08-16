package com.mascill.keutrack.core.designsystem.component.snackbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.State
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.mascill.keutrack.core.designsystem.component.snackbar.model.BaseKeuTrackSnackbarData
import com.mascill.keutrack.core.designsystem.component.snackbar.util.toMillis
import kotlinx.coroutines.delay

private const val SNACKBAR_FADE_IN_MILLIS = 150
private const val SNACKBAR_FADE_OUT_MILLIS = 75
private const val SNACKBAR_IN_BETWEEN_DELAY_MILLIS = 0
private const val SNACKBAR_SCALE_COLLAPSED = 0.8f

/**
 * Host that manages duration and fade/scale animation for [KeuTrackSnackbar].
 */
@Composable
fun KeuTrackSnackbarHost(
    hostState: KeuTrackSnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable BoxScope.(BaseKeuTrackSnackbarData) -> Unit,
) {
    val currentSnackbarData = hostState.currentSnackbarData
    val accessibilityManager = LocalAccessibilityManager.current

    LaunchedEffect(currentSnackbarData) {
        if (currentSnackbarData != null) {
            val duration = currentSnackbarData.data.duration.toMillis(
                currentSnackbarData.data.actionLabel != null,
                accessibilityManager,
            )
            delay(duration)
            currentSnackbarData.dismiss()
        }
    }

    FadeInFadeOutWithScale(
        current = hostState.currentSnackbarData,
        modifier = modifier,
        content = snackbar,
    )
}

/**
 * Full-screen overlay that renders the current snackbar from [hostState].
 */
@Composable
fun KeuTrackSnackbarOverlay(hostState: KeuTrackSnackbarHostState) {
    KeuTrackSnackbarHost(
        hostState = hostState,
        modifier = Modifier.fillMaxSize(),
    ) {
        KeuTrackSnackbar(
            data = it.data,
            position = it.position,
            onAction = { it.performAction() },
        )
    }
}

@Composable
private fun FadeInFadeOutWithScale(
    current: BaseKeuTrackSnackbarData?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(BaseKeuTrackSnackbarData) -> Unit,
) {
    val state = remember { FadeInFadeOutState<BaseKeuTrackSnackbarData?>() }
    if (current != state.current) {
        state.current = current
        val keys = state.items.map { it.key }.toMutableList()
        if (!keys.contains(current)) {
            keys.add(current)
        }
        state.items.clear()
        state.items += keys.filterNotNull().map { key ->
            FadeInFadeOutAnimationItem(key) { children ->
                val isVisible = key == current
                val duration = if (isVisible) SNACKBAR_FADE_IN_MILLIS else SNACKBAR_FADE_OUT_MILLIS
                val delay = SNACKBAR_FADE_OUT_MILLIS + SNACKBAR_IN_BETWEEN_DELAY_MILLIS
                val animationDelay = if (isVisible && keys.filterNotNull().size != 1) {
                    delay
                } else {
                    0
                }
                val opacity = animatedOpacity(
                    animation = tween(
                        easing = LinearEasing,
                        delayMillis = animationDelay,
                        durationMillis = duration,
                    ),
                    visible = isVisible,
                    onAnimationFinish = {
                        if (key != state.current) {
                            state.items.removeAll { it.key == key }
                            state.scope?.invalidate()
                        }
                    },
                )
                val scale = animatedScale(
                    animation = tween(
                        easing = FastOutSlowInEasing,
                        delayMillis = animationDelay,
                        durationMillis = duration,
                    ),
                    visible = isVisible,
                )
                Box(
                    modifier
                        .graphicsLayer(
                            scaleX = scale.value,
                            scaleY = scale.value,
                            alpha = opacity.value,
                        )
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            dismiss { key.dismiss(); true }
                        },
                ) {
                    children()
                }
            }
        }
    }
    Box(modifier) {
        state.scope = currentRecomposeScope
        state.items.forEach { (item, opacity) ->
            key(item) {
                opacity {
                    content(item!!)
                }
            }
        }
    }
}

private class FadeInFadeOutState<T> {
    var current: Any? = Any()
    var items = mutableListOf<FadeInFadeOutAnimationItem<T>>()
    var scope: RecomposeScope? = null
}

private data class FadeInFadeOutAnimationItem<T>(
    val key: T,
    val transition: FadeInFadeOutTransition,
)

private typealias FadeInFadeOutTransition =
    @Composable BoxScope.(content: @Composable () -> Unit) -> Unit

@Composable
private fun animatedOpacity(
    animation: AnimationSpec<Float>,
    visible: Boolean,
    onAnimationFinish: () -> Unit = {},
): State<Float> {
    val alpha = remember { Animatable(if (!visible) 1f else 0f) }
    LaunchedEffect(visible) {
        alpha.animateTo(
            if (visible) 1f else 0f,
            animationSpec = animation,
        )
        onAnimationFinish()
    }
    return alpha.asState()
}

@Composable
private fun animatedScale(animation: AnimationSpec<Float>, visible: Boolean): State<Float> {
    val scale = remember { Animatable(if (!visible) 1f else SNACKBAR_SCALE_COLLAPSED) }
    LaunchedEffect(visible) {
        scale.animateTo(
            if (visible) 1f else SNACKBAR_SCALE_COLLAPSED,
            animationSpec = animation,
        )
    }
    return scale.asState()
}
