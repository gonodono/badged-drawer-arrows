package com.gonodono.bda.compose

import android.graphics.PointF
import androidx.annotation.FloatRange
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.DrawableCompat.setLayoutDirection
import com.gonodono.bda.view.BadgedDrawerArrowDrawable
import com.gonodono.bda.view.plus
import kotlin.math.roundToInt

@Composable
fun BaseBadgedDrawerArrow(
    @FloatRange(0.0, 1.0) progress: Float,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Transparent,
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
    indication: Indication? = null,
    onClick: (() -> Unit)? = null
) {
    val click = if (onClick != null) {
        modifier.clickable(
            interactionSource = interactionSource,
            indication = indication,
            role = Role.Button,
            onClick = onClick
        )
    } else {
        modifier
    }

    val context = LocalContext.current
    val drawable = remember(context, LocalConfiguration.current) {
        BadgedDrawerArrowDrawable(context)
    }

    val clickAndDraw = click.drawWithCache {
        val right = size.width.roundToInt()
        val bottom = size.height.roundToInt()
        drawable.setBounds(0, 0, right, bottom)

        drawable.color = barColor.toArgb()
        drawable.barLength = barLength.toPx()
        drawable.barThickness = barThickness.toPx()
        drawable.gapSize = barGapSize.toPx()
        drawable.arrowShaftLength = arrowShaftLength.toPx()
        drawable.arrowHeadLength = arrowHeadLength.toPx()
        drawable.direction = arrowDirection.toDrawableArrowDirection()
        drawable.isSpinEnabled = isSpinEnabled
        setLayoutDirection(drawable, layoutDirection.ordinal)

        drawable.isBadgeEnabled = isBadgeEnabled
        drawable.badgeSize = badgeSize.toDrawableSize()
        drawable.badgeColor = badgeColor.toArgb()
        drawable.badgeCorner = badgeCorner.toDrawableCorner()
        drawable.badgeOffset = badgeOffset.toPointF()
        drawable.badgeClipMargin = badgeClipMargin.toPx()
        drawable.badgeText = badgeText
        drawable.badgeTextSize = badgeTextSize
        drawable.badgeTextColor = badgeTextColor.toArgb()
        drawable.badgeTextOffset = badgeTextOffset.toPointF()
        drawable.badgeMotion = badgeMotion.toDrawableMotion()
        drawable.autoMirrorOnReverse = autoMirrorOnReverse

        onDrawBehind {
            drawable.progress = progress
            drawable.draw(drawContext.canvas.nativeCanvas)
        }
    }

    Box(modifier = clickAndDraw)
}

enum class ArrowDirection {

    Left, Right, Start, End;

    @Stable
    internal fun toDrawableArrowDirection(): Int = ordinal
}

@Immutable
sealed class BadgeSize {
    data object Normal : BadgeSize()
    data object Dot : BadgeSize()
    data class Custom(val diameter: Float) : BadgeSize()

    internal fun toDrawableSize(): BadgedDrawerArrowDrawable.BadgeSize =
        when (this) {
            Normal -> BadgedDrawerArrowDrawable.BadgeSize.Normal
            Dot -> BadgedDrawerArrowDrawable.BadgeSize.Dot
            is Custom -> BadgedDrawerArrowDrawable.BadgeSize.Custom(diameter)
        }
}

enum class Corner {

    TopLeft, TopRight, BottomRight, BottomLeft;

    @Stable
    internal fun toDrawableCorner(): BadgedDrawerArrowDrawable.Corner =
        BadgedDrawerArrowDrawable.Corner.entries[ordinal]
}

@Immutable
sealed class Motion(internal val drawableMotion: BadgedDrawerArrowDrawable.Motion) {

    data object None : Motion(BadgedDrawerArrowDrawable.Motion.None)
    data object Grow : Motion(BadgedDrawerArrowDrawable.Motion.Grow)
    data object Shrink : Motion(BadgedDrawerArrowDrawable.Motion.Shrink)

    data object FullSpinCW : Motion(BadgedDrawerArrowDrawable.Motion.FullSpinCW)
    data object FullSpinCCW :
        Motion(BadgedDrawerArrowDrawable.Motion.FullSpinCCW)

    data object HalfSpinCW : Motion(BadgedDrawerArrowDrawable.Motion.HalfSpinCW)
    data object HalfSpinCCW :
        Motion(BadgedDrawerArrowDrawable.Motion.HalfSpinCCW)

    operator fun plus(other: Motion): Motion =
        CombinedMotion(drawableMotion, other.drawableMotion)

    internal class CombinedMotion(
        first: BadgedDrawerArrowDrawable.Motion,
        second: BadgedDrawerArrowDrawable.Motion
    ) : Motion(first + second) {

        override fun toString(): String = drawableMotion.toString()
    }

    internal fun toDrawableMotion(): BadgedDrawerArrowDrawable.Motion =
        drawableMotion

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Motion) return false
        return drawableMotion == other.drawableMotion
    }

    override fun hashCode(): Int = drawableMotion.hashCode()
}

@Immutable
object BadgedDrawerArrowDefaults {
    val BarLength = 18.dp
    val BarThickness = 2.dp
    val BarGapSize = 3.dp
    val ArrowShaftLength = 16.dp
    val ArrowHeadLength = 8.dp
}

val DotDiameter: Dp = BadgedDrawerArrowDrawable.DOT_DIAMETER_DP.dp  // 8.dp

@Suppress("NOTHING_TO_INLINE")
private inline fun Offset.toPointF() = PointF(x, y)