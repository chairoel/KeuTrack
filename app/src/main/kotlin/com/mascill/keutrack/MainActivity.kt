package com.mascill.keutrack

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import dagger.hilt.android.AndroidEntryPoint

/**
 * Base activity class that handle compose features.
 *
 * @see ComponentActivity
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        var keepSplashOnScreen = true
        installSplashScreen().apply {
            setKeepOnScreenCondition { keepSplashOnScreen }
            setOnExitAnimationListener { screen -> splashZoomAnimation(screen = screen) }
        }
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        setContent {
            SideEffect { keepSplashOnScreen = false }
            KeuTrackAppScreen()
        }
    }

    private fun applyEdgeToEdge() {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
            ),
        )
    }

    private fun splashZoomAnimation(screen: SplashScreenViewProvider) {
        val iconView = runCatching { screen.iconView }.getOrNull()
        if (iconView == null || !iconView.isAttachedToWindow) {
            screen.remove()
            applyEdgeToEdge()
            return
        }

        val animDuration = 700L
        val startScale = 1.0f
        val endScale = 1.5f
        val fadeOut = ObjectAnimator.ofFloat(
            iconView,
            View.ALPHA,
            1.0f,
            0f
        )
        fadeOut.duration = animDuration

        val zoomX = ObjectAnimator.ofFloat(
            iconView,
            View.SCALE_X,
            startScale,
            endScale
        )
        zoomX.interpolator = OvershootInterpolator()
        zoomX.duration = animDuration

        val zoomY = ObjectAnimator.ofFloat(
            iconView,
            View.SCALE_Y,
            startScale,
            endScale
        )
        zoomY.interpolator = OvershootInterpolator()
        zoomY.duration = animDuration

        zoomX.doOnEnd { fadeOut.start() }
        fadeOut.doOnEnd {
            screen.remove()
            applyEdgeToEdge()
        }

        zoomX.start()
        zoomY.start()
    }
}