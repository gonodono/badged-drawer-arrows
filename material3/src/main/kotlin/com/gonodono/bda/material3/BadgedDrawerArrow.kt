package com.gonodono.bda.material3

import androidx.annotation.FloatRange
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gonodono.bda.compose.ArrowDirection
import com.gonodono.bda.compose.BadgeSize
import com.gonodono.bda.compose.BaseBadgedDrawerArrow
import com.gonodono.bda.compose.BadgedDrawerArrowDefaults
import com.gonodono.bda.compose.Corner
import com.gonodono.bda.compose.Motion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BadgedDrawerArrow(
    @FloatRange(0.0, 1.0) progress: Float,
    modifier: Modifier = Modifier,
    barColor: Color = LocalContentColor.current,
    barLength: Dp = BadgedDrawerArrowDefaults.BarLength,
    barThickness: Dp = BadgedDrawerArrowDefaults.BarThickness,
    barGapSize: Dp = BadgedDrawerArrowDefaults.BarGapSize,
    arrowShaftLength: Dp = BadgedDrawerArrowDefaults.ArrowShaftLength,
    arrowHeadLength: Dp = BadgedDrawerArrowDefaults.ArrowHeadLength,
    arrowDirection: ArrowDirection = ArrowDirection.Start,
    isSpinEnabled: Boolean = true,
    isBadgeEnabled: Boolean = false,
    badgeSize: BadgeSize = BadgeSize.Normal,
    badgeColor: Color = Color.Red,
    badgeCorner: Corner = Corner.TopRight,
    badgeOffset: Offset = Offset.Zero,
    badgeClipMargin: Dp = 0.dp,
    badgeText: String? = null,
    badgeTextSize: (default: Float) -> Float = { it },
    badgeTextColor: Color = Color.White,
    badgeTextOffset: Offset = Offset.Zero,
    badgeMotion: Motion = Motion.None,
    autoMirrorOnReverse: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = DefaultRipple,
    onClick: (() -> Unit)? = null
) {
    BaseBadgedDrawerArrow(
        progress = progress,
        modifier = modifier.minimumInteractiveComponentSize(),
        barColor = barColor,
        barLength = barLength,
        barThickness = barThickness,
        barGapSize = barGapSize,
        arrowShaftLength = arrowShaftLength,
        arrowHeadLength = arrowHeadLength,
        arrowDirection = arrowDirection,
        isSpinEnabled = isSpinEnabled,
        isBadgeEnabled = isBadgeEnabled,
        badgeSize = badgeSize,
        badgeColor = badgeColor,
        badgeCorner = badgeCorner,
        badgeOffset = badgeOffset,
        badgeClipMargin = badgeClipMargin,
        badgeText = badgeText,
        badgeTextSize = badgeTextSize,
        badgeTextColor = badgeTextColor,
        badgeTextOffset = badgeTextOffset,
        badgeMotion = badgeMotion,
        autoMirrorOnReverse = autoMirrorOnReverse,
        interactionSource = interactionSource,
        indication = indication,
        onClick = onClick
    )
}

@Composable
fun BadgedDrawerArrow(
    drawerToggle: DrawerToggle,
    modifier: Modifier = Modifier,
    barColor: Color = LocalContentColor.current,
    barLength: Dp = BadgedDrawerArrowDefaults.BarLength,
    barThickness: Dp = BadgedDrawerArrowDefaults.BarThickness,
    barGapSize: Dp = BadgedDrawerArrowDefaults.BarGapSize,
    arrowShaftLength: Dp = BadgedDrawerArrowDefaults.ArrowShaftLength,
    arrowHeadLength: Dp = BadgedDrawerArrowDefaults.ArrowHeadLength,
    arrowDirection: ArrowDirection = ArrowDirection.Start,
    isSpinEnabled: Boolean = true,
    isBadgeEnabled: Boolean = false,
    badgeSize: BadgeSize = BadgeSize.Normal,
    badgeColor: Color = Color.Red,
    badgeCorner: Corner = Corner.TopRight,
    badgeOffset: Offset = Offset.Zero,
    badgeClipMargin: Dp = 0.dp,
    badgeText: String? = null,
    badgeTextSize: (default: Float) -> Float = { it },
    badgeTextColor: Color = Color.White,
    badgeTextOffset: Offset = Offset.Zero,
    badgeMotion: Motion = Motion.None,
    autoMirrorOnReverse: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = DefaultRipple
) {
    BaseBadgedDrawerArrow(
        progress = drawerToggle.progress(),
        modifier = modifier.minimumInteractiveComponentSize(),
        barColor = barColor,
        barLength = barLength,
        barThickness = barThickness,
        barGapSize = barGapSize,
        arrowShaftLength = arrowShaftLength,
        arrowHeadLength = arrowHeadLength,
        arrowDirection = arrowDirection,
        isSpinEnabled = isSpinEnabled,
        isBadgeEnabled = isBadgeEnabled,
        badgeSize = badgeSize,
        badgeColor = badgeColor,
        badgeCorner = badgeCorner,
        badgeOffset = badgeOffset,
        badgeClipMargin = badgeClipMargin,
        badgeText = badgeText,
        badgeTextSize = badgeTextSize,
        badgeTextColor = badgeTextColor,
        badgeTextOffset = badgeTextOffset,
        badgeMotion = badgeMotion,
        autoMirrorOnReverse = autoMirrorOnReverse,
        interactionSource = interactionSource,
        indication = indication,
        onClick = { drawerToggle.toggle() }
    )
}

@Stable
interface DrawerToggle {

    @Stable
    val drawerState: DrawerState

    @Stable
    fun progress(): Float

    fun toggle()
}

@Composable
fun rememberDrawerToggle(
    drawerWidth: Dp,
    initialValue: DrawerValue = DrawerValue.Closed,
    confirmStateChange: (DrawerValue) -> Boolean = { true }
): DrawerToggle {
    val drawerState = rememberDrawerState(initialValue, confirmStateChange)
    return rememberDrawerToggle(drawerWidth, drawerState)
}

@Composable
fun rememberDrawerToggle(
    drawerWidth: Dp,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
): DrawerToggle {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    return remember(drawerState, drawerWidth, density) {
        DrawerToggleImpl(drawerState, drawerWidth, scope, density)
    }
}

private class DrawerToggleImpl(
    override val drawerState: DrawerState,
    private val drawerWidth: Dp,
    private val scope: CoroutineScope,
    density: Density
) : DrawerToggle, Density by density {

    override fun progress(): Float = progress(drawerState, drawerWidth)

    override fun toggle() {
        scope.launch { drawerState.toggle() }
    }
}

@Composable
fun DrawerState.progress(drawerWidth: Dp): Float =
    with(LocalDensity.current) { progress(this@progress, drawerWidth) }

@Suppress("NOTHING_TO_INLINE")
private inline fun Density.progress(
    drawerState: DrawerState,
    drawerWidth: Dp
): Float = 1F - drawerState.currentOffset / -drawerWidth.toPx()

suspend fun DrawerState.toggle() = if (isOpen) close() else open()

val DefaultRipple: IndicationNodeFactory = ripple(false, 20.dp)