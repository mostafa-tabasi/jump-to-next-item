package com.mstf.jumptonextitem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


inline fun Modifier.conditional(
    condition: Boolean,
    ifTrue: Modifier.() -> Modifier,
    ifFalse: Modifier.() -> Modifier = { this },
): Modifier = if (condition) {
    then(ifTrue(Modifier))
} else {
    then(ifFalse(Modifier))
}

fun Density.dpToPx(dp: Dp): Float = with(this) { dp.toPx() }

fun Density.pxToDp(px: Float): Dp = with(this) { px.toDp() }

fun <T, V : AnimationVector> Animatable<T, V>.tweenAnimateTo(
    scope: CoroutineScope,
    targetValue: T,
    duration: Int = DefaultDurationMillis,
) {
    scope.launch {
        animateTo(
            targetValue,
            animationSpec = tween(duration),
        )
    }
}

fun <T, V : AnimationVector> Animatable<T, V>.springAnimateTo(
    scope: CoroutineScope,
    targetValue: T,
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
) {
    scope.launch {
        animateTo(
            targetValue,
            animationSpec = spring(dampingRatio, stiffness),
        )
    }
}

fun <T, V : AnimationVector> Animatable<T, V>.snapTo(
    scope: CoroutineScope,
    targetValue: T,
) {
    scope.launch { snapTo(targetValue) }
}