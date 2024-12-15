package com.gonodono.bda.compose

import android.graphics.PointF
import androidx.annotation.FloatRange
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.DrawableCompat.setLayoutDirection
import com.gonodono.bda.view.BadgedDrawerArrowDrawable
import com.gonodono.bda.view.plus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.gonodono.bda.view.BadgedDrawerArrowDrawable.Motion as DrawableMotion

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
sealed class Motion(internal val drawableMotion: DrawableMotion) {

    data object None : Motion(DrawableMotion.None)
    data object Grow : Motion(DrawableMotion.Grow)
    data object Shrink : Motion(DrawableMotion.Shrink)
    data object FullSpinCW : Motion(DrawableMotion.FullSpinCW)
    data object FullSpinCCW : Motion(DrawableMotion.FullSpinCCW)
    data object HalfSpinCW : Motion(DrawableMotion.HalfSpinCW)
    data object HalfSpinCCW : Motion(DrawableMotion.HalfSpinCCW)

    operator fun plus(other: Motion): Motion =
        CombinedMotion(drawableMotion, other.drawableMotion)

    internal class CombinedMotion(
        first: DrawableMotion,
        second: DrawableMotion
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
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val drawable = remember(context, LocalConfiguration.current) {
        BadgedDrawerArrowDrawable(context)
    }
    val contentColor = LocalContentColor.current

    IconButton(
        onClick = onClick,
        modifier = modifier.drawWithCache {

            val right = size.width.roundToInt()
            val bottom = size.height.roundToInt()
            drawable.setBounds(0, 0, right, bottom)

            drawable.color = barColor.takeOrElse { contentColor }.toArgb()
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

            onDrawWithContent {
                drawable.progress = progress
                drawable.draw(drawContext.canvas.nativeCanvas)
            }
        }
    ) {}
}

@Composable
fun BadgedDrawerArrow(
    drawerToggle: DrawerToggle,
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
    autoMirrorOnReverse: Boolean = true
) {
    BadgedDrawerArrow(
        progress = drawerToggle.progress(),
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
        badgeTextSize = badgeTextSize,
        badgeTextColor = badgeTextColor,
        badgeTextOffset = badgeTextOffset,
        badgeMotion = badgeMotion,
        autoMirrorOnReverse = autoMirrorOnReverse,
        onClick = drawerToggle::toggle
    )
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

@Stable
interface DrawerToggle {

    @Stable
    val drawerState: DrawerState

    @Stable
    fun progress(): Float

    fun toggle()
}

private class DrawerToggleImpl(
    override val drawerState: DrawerState,
    private val drawerWidth: Dp,
    private val scope: CoroutineScope,
    density: Density
) : DrawerToggle, Density by density {

    override fun progress(): Float =
        progress(drawerState, drawerWidth)

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

val DotDiameter = 8.dp

@Immutable
object BadgedDrawerArrowDefaults {
    val BarLength = 18.dp
    val BarThickness = 2.dp
    val BarGapSize = 3.dp
    val ArrowShaftLength = 16.dp
    val ArrowHeadLength = 8.dp
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Offset.toPointF() = PointF(x, y)