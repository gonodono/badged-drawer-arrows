package com.gonodono.bda.compose

import android.graphics.PointF
import androidx.annotation.FloatRange
import androidx.compose.material3.DrawerState
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gonodono.bda.view.BadgedDrawerArrowDrawable
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.gonodono.bda.view.BadgedDrawerArrowDrawable.Animation as DrawableAnimation

enum class ArrowDirection {

    Left, Right, Start, End;

    fun toDrawableArrowDirection(): Int = ordinal
}

@Immutable
sealed class BadgeSize {
    data object Standard : BadgeSize()
    data object Dot : BadgeSize()
    data class Custom(val size: Float) : BadgeSize()

    internal fun toDrawableSizeMode(): BadgedDrawerArrowDrawable.BadgeSize =
        when (this) {
            Standard -> BadgedDrawerArrowDrawable.BadgeSize.Standard
            Dot -> BadgedDrawerArrowDrawable.BadgeSize.Dot
            is Custom -> BadgedDrawerArrowDrawable.BadgeSize.Custom(size)
        }
}

enum class Corner {

    TopLeft, TopRight, BottomRight, BottomLeft;

    internal fun toDrawableCorner(): BadgedDrawerArrowDrawable.Corner =
        BadgedDrawerArrowDrawable.Corner.entries[ordinal]
}

@Immutable
sealed class Animation(internal val drawableAnimation: DrawableAnimation) {

    data object None : Animation(DrawableAnimation.None)
    data object Grow : Animation(DrawableAnimation.Grow)
    data object Shrink : Animation(DrawableAnimation.Shrink)
    data object FullSpinCW : Animation(DrawableAnimation.FullSpinCW)
    data object FullSpinCCW : Animation(DrawableAnimation.FullSpinCCW)
    data object HalfSpinCW : Animation(DrawableAnimation.HalfSpinCW)
    data object HalfSpinCCW : Animation(DrawableAnimation.HalfSpinCCW)

    operator fun plus(other: Animation): Animation =
        CombinedAnimation(drawableAnimation, other.drawableAnimation)

    internal class CombinedAnimation(
        first: DrawableAnimation,
        second: DrawableAnimation
    ) : Animation(first + second) {
        override fun toString(): String = drawableAnimation.toString()
    }

    internal fun toDrawableAnimation(): BadgedDrawerArrowDrawable.Animation =
        drawableAnimation
}

@Composable
fun BadgedDrawerArrow(
    @FloatRange(0.0, 1.0) progress: Float,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Unspecified,
    barLength: Dp = BadgedDrawerArrowDefaults.BarLength,
    barThickness: Dp = BadgedDrawerArrowDefaults.BarThickness,
    barGapSize: Dp = BadgedDrawerArrowDefaults.BarGapSize,
    arrowShaftLength: Dp = BadgedDrawerArrowDefaults.ArrowShaftLength,
    arrowHeadLength: Dp = BadgedDrawerArrowDefaults.ArrowHeadLength,
    arrowDirection: ArrowDirection = ArrowDirection.Start,
    isSpinEnabled: Boolean = true,
    isBadgeEnabled: Boolean = false,
    badgeSize: BadgeSize = BadgeSize.Standard,
    badgeColor: Color = BadgedDrawerArrowDefaults.BadgeColor,
    badgeCorner: Corner = Corner.TopRight,
    badgeOffset: Offset = Offset.Zero,
    badgeClipMargin: Dp = 0.dp,
    badgeText: String? = null,
    badgeTextColor: Color = BadgedDrawerArrowDefaults.BadgeTextColor,
    badgeTextOffset: Offset = Offset.Zero,
    badgeAnimation: Animation = Animation.None,
    autoMirrorOnReverse: Boolean = false,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val drawable = remember { BadgedDrawerArrowDrawable(context) }
    drawable.progress = progress
    drawable.color = barColor.takeOrElse { LocalContentColor.current }.toArgb()
    drawable.direction = arrowDirection.toDrawableArrowDirection()
    drawable.isSpinEnabled = isSpinEnabled
    drawable.isBadgeEnabled = isBadgeEnabled
    drawable.badgeSize = badgeSize.toDrawableSizeMode()
    drawable.badgeColor = badgeColor.toArgb()
    drawable.badgeCorner = badgeCorner.toDrawableCorner()
    drawable.badgeOffset = badgeOffset.run { PointF(x, y) }
    drawable.badgeText = badgeText
    drawable.badgeTextColor = badgeTextColor.toArgb()
    drawable.badgeTextOffset = badgeTextOffset.run { PointF(x, y) }
    drawable.badgeAnimation = badgeAnimation.toDrawableAnimation()
    drawable.autoMirrorOnReverse = autoMirrorOnReverse
    with(LocalDensity.current) {
        drawable.barLength = barLength.toPx()
        drawable.barThickness = barThickness.toPx()
        drawable.gapSize = barGapSize.toPx()
        drawable.arrowHeadLength = arrowHeadLength.toPx()
        drawable.arrowShaftLength = arrowShaftLength.toPx()
        drawable.badgeClipMargin = badgeClipMargin.toPx()
    }
    IconButton(
        onClick = onClick,
        modifier = modifier.drawWithCache {
            val right = size.width.roundToInt()
            val bottom = size.height.roundToInt()
            drawable.setBounds(0, 0, right, bottom)
            onDrawWithContent { drawable.draw(drawContext.canvas.nativeCanvas) }
        }
    ) {}
}

@Composable
fun BadgedDrawerArrow(
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Unspecified,
    barLength: Dp = BadgedDrawerArrowDefaults.BarLength,
    barThickness: Dp = BadgedDrawerArrowDefaults.BarThickness,
    barGapSize: Dp = BadgedDrawerArrowDefaults.BarGapSize,
    arrowShaftLength: Dp = BadgedDrawerArrowDefaults.ArrowShaftLength,
    arrowHeadLength: Dp = BadgedDrawerArrowDefaults.ArrowHeadLength,
    arrowDirection: ArrowDirection = ArrowDirection.Start,
    isSpinEnabled: Boolean = true,
    isBadgeEnabled: Boolean = false,
    badgeSize: BadgeSize = BadgeSize.Standard,
    badgeColor: Color = BadgedDrawerArrowDefaults.BadgeColor,
    badgeCorner: Corner = Corner.TopRight,
    badgeOffset: Offset = Offset.Zero,
    badgeClipMargin: Dp = 0.dp,
    badgeText: String? = null,
    badgeTextColor: Color = BadgedDrawerArrowDefaults.BadgeTextColor,
    badgeTextOffset: Offset = Offset.Zero,
    badgeAnimation: Animation = Animation.None,
    autoMirrorOnReverse: Boolean = true
) {
    val scope = rememberCoroutineScope()
    BadgedDrawerArrow(
        progress = drawerState.progress(),
        modifier = modifier,
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
        badgeTextColor = badgeTextColor,
        badgeTextOffset = badgeTextOffset,
        badgeAnimation = badgeAnimation,
        autoMirrorOnReverse = autoMirrorOnReverse,
        onClick = { scope.launch { drawerState.toggle() } }
    )
}

// From NavigationDrawerTokens.ContainerWidth
val NavigationDrawerContainerWidth = 360.dp

@Composable
fun DrawerState.progress(): Float = with(LocalDensity.current) {
    1F - currentOffset / -NavigationDrawerContainerWidth.toPx()
}

suspend inline fun DrawerState.toggle() = if (isOpen) close() else open()

object BadgedDrawerArrowDefaults {
    val BarLength = 18.dp
    val BarThickness = 2.dp
    val BarGapSize = 3.dp
    val ArrowShaftLength = 16.dp
    val ArrowHeadLength = 8.dp
    val BadgeColor = Color.Red
    val BadgeTextColor = Color.White
}