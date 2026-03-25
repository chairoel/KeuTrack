package com.mascill.keutrack

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        installSplashScreen().apply {
            setOnExitAnimationListener { screen -> splashZoomAnimation(screen = screen) }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KeuTrackAppScreen() }
    }

    private fun splashZoomAnimation(screen: SplashScreenViewProvider) {
        val animDuration = 700L
        val startScale = 1.0f
        val endScale = 1.5f
        val fadeOut = ObjectAnimator.ofFloat(
            screen.iconView,
            View.ALPHA,
            1.0f,
            0f
        )
        fadeOut.duration = animDuration

        val zoomX = ObjectAnimator.ofFloat(
            screen.iconView,
            View.SCALE_X,
            startScale,
            endScale
        )
        zoomX.interpolator = OvershootInterpolator()
        zoomX.duration = animDuration

        val zoomY = ObjectAnimator.ofFloat(
            screen.iconView,
            View.SCALE_Y,
            startScale,
            endScale
        )
        zoomY.interpolator = OvershootInterpolator()
        zoomY.duration = animDuration

        zoomX.doOnEnd { fadeOut.start() } // start fade out after scaling
        fadeOut.doOnEnd { screen.remove() } // remove screen after fade out animation end

        zoomX.start()
        zoomY.start()
    }
}