package com.mascill.keutrack.core.common.utils

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

private const val NAV_ANIM_DURATION = 300 // milisecond

/**
 * Compose navigation slide left with fade in Animation
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideLeftWithFadeInAnimation(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = NAV_ANIM_DURATION, easing = LinearEasing
        )
    ) + slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(NAV_ANIM_DURATION)
    )

/**
 * Compose navigation slide left with fade out Animation
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideLeftWithFadeOutAnimation(): ExitTransition =
    fadeOut(
        animationSpec = tween(
            durationMillis = NAV_ANIM_DURATION, easing = LinearEasing
        )
    ) + slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(NAV_ANIM_DURATION)
    )

/**
 * Compose navigation slide right with fade in Animation
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideRightWithFadeInAnimation(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = NAV_ANIM_DURATION, easing = LinearEasing
        )
    ) + slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(NAV_ANIM_DURATION)
    )

/**
 * Compose navigation slide right with fade out Animation
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideRightWithFadeOutAnimation(): ExitTransition =
    fadeOut(
        animationSpec = tween(
            durationMillis = NAV_ANIM_DURATION, easing = LinearEasing
        )
    ) + slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(NAV_ANIM_DURATION)
    )