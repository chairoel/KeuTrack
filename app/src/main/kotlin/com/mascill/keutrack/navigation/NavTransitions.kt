package com.mascill.keutrack.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.mascill.keutrack.feature.auth.navigation.LoginRoute
import com.mascill.keutrack.feature.auth.navigation.RegisterRoute
import com.mascill.keutrack.feature.splashscreen.navigation.SplashRoute
import com.mascill.keutrack.feature.transaction.navigation.TransactionRoute

/**
 * Shared enter/exit specs for the top-level [KeuTrackNavHost] and nested Home tabs.
 */
object NavTransitions {
    const val SLIDE_DURATION_MS = 300
    const val FADE_DURATION_MS = 200
    const val TAB_FADE_DURATION_MS = 300
    const val SPLASH_FADE_DURATION_MS = 300

    val slideIn = slideInHorizontally(
        animationSpec = tween(SLIDE_DURATION_MS),
        initialOffsetX = { it },
    )
    val slideOut = slideOutHorizontally(
        animationSpec = tween(SLIDE_DURATION_MS),
        targetOffsetX = { -it },
    )
    val popSlideIn = slideInHorizontally(
        animationSpec = tween(SLIDE_DURATION_MS),
        initialOffsetX = { -it },
    )
    val popSlideOut = slideOutHorizontally(
        animationSpec = tween(SLIDE_DURATION_MS),
        targetOffsetX = { it },
    )
    val fadeIn = fadeIn(animationSpec = tween(FADE_DURATION_MS))
    val fadeOut = fadeOut(animationSpec = tween(FADE_DURATION_MS))
    val splashFadeIn = fadeIn(animationSpec = tween(SPLASH_FADE_DURATION_MS))
    val splashFadeOut = fadeOut(animationSpec = tween(SPLASH_FADE_DURATION_MS))
    val tabFadeIn = fadeIn(animationSpec = tween(TAB_FADE_DURATION_MS))
    val tabFadeOut = fadeOut(animationSpec = tween(TAB_FADE_DURATION_MS))
    val slideInUp = slideInVertically(
        animationSpec = tween(SLIDE_DURATION_MS),
        initialOffsetY = { it },
    )
    val slideOutDown = slideOutVertically(
        animationSpec = tween(SLIDE_DURATION_MS),
        targetOffsetY = { it },
    )

    fun enter(
        scope: AnimatedContentTransitionScope<NavBackStackEntry>,
    ): EnterTransition {
        val initial = scope.initialState.destination
        val target = scope.targetState.destination
        return when {
            initial.hasRoute<SplashRoute>() -> splashFadeIn
            isAuth(initial) || isAuth(target) -> fadeIn
            target.hasRoute<TransactionRoute>() -> slideInUp
            else -> slideIn
        }
    }

    fun exit(
        scope: AnimatedContentTransitionScope<NavBackStackEntry>,
    ): ExitTransition {
        val initial = scope.initialState.destination
        val target = scope.targetState.destination
        return when {
            initial.hasRoute<SplashRoute>() -> splashFadeOut
            isAuth(initial) || isAuth(target) -> fadeOut
            target.hasRoute<TransactionRoute>() -> fadeOut
            else -> slideOut
        }
    }

    fun popEnter(
        scope: AnimatedContentTransitionScope<NavBackStackEntry>,
    ): EnterTransition {
        val initial = scope.initialState.destination
        val target = scope.targetState.destination
        return when {
            isAuth(initial) || isAuth(target) -> fadeIn
            initial.hasRoute<TransactionRoute>() -> fadeIn
            else -> popSlideIn
        }
    }

    fun popExit(
        scope: AnimatedContentTransitionScope<NavBackStackEntry>,
    ): ExitTransition {
        val initial = scope.initialState.destination
        val target = scope.targetState.destination
        return when {
            isAuth(initial) || isAuth(target) -> fadeOut
            initial.hasRoute<TransactionRoute>() -> slideOutDown
            else -> popSlideOut
        }
    }

    private fun isAuth(destination: NavDestination): Boolean =
        destination.hasRoute<LoginRoute>() || destination.hasRoute<RegisterRoute>()
}
