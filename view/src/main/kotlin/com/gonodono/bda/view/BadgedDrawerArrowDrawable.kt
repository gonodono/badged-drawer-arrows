package com.gonodono.bda.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Region
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.graphics.withTranslation
import kotlin.properties.Delegates

class BadgedDrawerArrowDrawable(context: Context) :
    DrawerArrowDrawable(context) {

    var isBadgeEnabled: Boolean by invalidating(false, invalidateClip = true)

    sealed class BadgeSize {
        data object Standard : BadgeSize()
        data object Dot : BadgeSize()
        data class Custom(val size: Float) : BadgeSize()
    }

    var badgeSize: BadgeSize by invalidating(BadgeSize.Standard, true)

    @get:ColorInt
    @setparam:ColorInt
    var badgeColor: Int by invalidating(Color.RED)

    enum class Corner { TopLeft, TopRight, BottomRight, BottomLeft }

    var badgeCorner: Corner by invalidating(Corner.TopRight, true)

    var badgeOffset: PointF by invalidating(PointF(), true)

    var badgeClipMargin: Float by invalidating(0F, true)

    var badgeText: String? by invalidating(null)

    @get:ColorInt
    @setparam:ColorInt
    var badgeTextColor: Int by invalidating(Color.WHITE)

    var badgeTextOffset: PointF by invalidating(PointF())

    private fun <T> invalidating(initial: T, invalidateClip: Boolean = false) =
        Delegates.observable(initial) { _, old, new ->
            if (old != new) {
                if (invalidateClip) isClipInvalidated = true
                invalidateSelf()
            }
        }

    sealed class Animation(
        internal val endScale: Float? = null,
        internal val endRotation: Float? = null
    ) {
        data object None : Animation()
        data object Grow : Animation(1.5F, null)
        data object Shrink : Animation(0F, null)
        data object FullSpinCW : Animation(null, 360F)
        data object FullSpinCCW : Animation(null, -360F)
        data object HalfSpinCW : Animation(null, 180F)
        data object HalfSpinCCW : Animation(null, -180F)

        operator fun plus(other: Animation): Animation =
            CombinedAnimation(this, other)

        internal class CombinedAnimation(
            private val first: Animation,
            private val second: Animation
        ) : Animation(
            second.endScale ?: first.endScale,
            second.endRotation ?: first.endRotation
        ) {
            override val ss: String?
                get() = second.run { endScale?.let { ss } } ?: first.ss
            override val rs: String?
                get() = second.run { endRotation?.let { rs } } ?: first.rs

            override fun toString(): String = buildString {
                ss?.let { append(it) }
                if (ss != null && rs != null) append("+")
                rs?.let { append(it) }
            }
        }

        internal open val ss: String? get() = toString()
        internal open val rs: String? get() = toString()
    }

    var badgeAnimation: Animation = Animation.None
        set(value) {
            if (field == value) return
            field = value
            calculateAnimation(progress)
        }

    var autoMirrorOnReverse: Boolean = false

    // This is figured at init so that we don't have to hang on the the Context.
    private val dotDiameter = DOT_DP * context.resources.displayMetrics.density

    val badgeDiameter: Float
        get() = when (val mode = badgeSize) {
            BadgeSize.Standard -> 3 * barThickness + 2 * gapSize
            BadgeSize.Dot -> dotDiameter
            is BadgeSize.Custom -> mode.size
        }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        isClipInvalidated = true
    }

    // NB: the super class exposes its own (synthetic) `paint` property.
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply { typeface = Typeface.DEFAULT_BOLD }

    override fun setAlpha(alpha: Int) {
        super.setAlpha(alpha)
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        super.setColorFilter(colorFilter)
        paint.setColorFilter(colorFilter)
    }

    override fun draw(canvas: Canvas) {
        val centerX = bounds.centerX() + when (badgeCorner) {
            Corner.TopLeft, Corner.BottomLeft -> -barLength / 2F
            else -> barLength / 2F
        } + badgeOffset.x
        val centerY = bounds.centerY() + when (badgeCorner) {
            Corner.TopLeft, Corner.TopRight -> -(1.5F * barThickness + gapSize)
            else -> 1.5F * barThickness + gapSize
        } + badgeOffset.y
        val radius = badgeDiameter / 2F

        // The super class doesn't handle its vertical bounds correctly, so we
        // translate the super draw here, and offset the clip by the same below.
        canvas.withTranslation(0F, bounds.top.toFloat()) {
            when (val clip = calculateClipPath(centerX, centerY, radius)) {
                null -> super.draw(canvas)
                else -> {
                    val count = canvas.save()
                    clipOutPath(canvas, clip)
                    super.draw(canvas)
                    canvas.restoreToCount(count)
                }
            }
        }
        if (isBadgeEnabled) drawBadge(canvas, paint, centerX, centerY, radius)
    }

    private var clipPath: Path? = null

    private var isClipInvalidated = false

    private fun calculateClipPath(
        centerX: Float,
        centerY: Float,
        radius: Float
    ): Path? {
        val path = when {
            isBadgeEnabled && badgeClipMargin > 0F -> {
                val path = clipPath ?: Path()
                when {
                    isClipInvalidated -> path.apply {
                        rewind()
                        val y = centerY - bounds.top  // 'cause of bug in super.
                        val scaledRadius = (radius + badgeClipMargin) * scale
                        addCircle(centerX, y, scaledRadius, Path.Direction.CW)
                        isClipInvalidated = false
                    }
                    else -> path
                }
            }
            else -> null
        }
        return path.also { clipPath = it }
    }

    private fun drawBadge(
        canvas: Canvas,
        paint: Paint,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {
        paint.color = badgeColor
        canvas.drawCircle(centerX, centerY, radius * scale, paint)

        if (badgeSize == BadgeSize.Dot) return
        val text = badgeText.takeIf { !it.isNullOrBlank() } ?: return

        val count = canvas.save()
        canvas.rotate(rotation, centerX, centerY)
        canvas.scale(scale, scale, centerX, centerY)

        val textBounds = tmpRect
        paint.textSize = badgeDiameter * textSizeFactor(text.length)
        paint.getTextBounds(text, 0, text.length, textBounds)

        val textX = centerX - textBounds.width() / 2F - 1
        val textY = centerY + textBounds.height() / 2F - 1
        val offsetX = textX + badgeTextOffset.x
        val offsetY = textY + badgeTextOffset.y
        paint.color = badgeTextColor
        canvas.drawText(text, offsetX, offsetY, paint)

        canvas.restoreToCount(count)
    }

    private var scale = 1F

    private var rotation = 0F

    private var verticalMirror = false

    override fun setProgress(progress: Float) {
        if (autoMirrorOnReverse) when (progress) {
            1F -> setVerticalMirror(true)
            0F -> setVerticalMirror(false)
        }
        super.setProgress(progress)
        calculateAnimation(progress)
    }

    override fun setVerticalMirror(verticalMirror: Boolean) {
        super.setVerticalMirror(verticalMirror)
        this.verticalMirror = verticalMirror
    }

    private fun calculateAnimation(progress: Float) {
        val newScale = when (val end = badgeAnimation.endScale) {
            null -> 1F
            else -> lerp(1F, end, progress)
        }
        val newRotation = when (val end = badgeAnimation.endRotation) {
            null -> 0F
            else -> lerp(0F, end, progress) * if (verticalMirror) -1 else 1
        }
        if (scale != newScale || rotation != newRotation) invalidateSelf()
        if (scale != newScale) isClipInvalidated = true
        rotation = newRotation
        scale = newScale
    }

    companion object {

        private const val DOT_DP = 8

        // This really only handles lengths of 1, 2, or 3. Tweak as needed.
        private fun textSizeFactor(textLength: Int): Float =
            when (textLength) {
                1 -> 0.75F
                2 -> 0.6F
                else -> 0.5F
            }

        private fun lerp(start: Float, end: Float, fraction: Float): Float =
            (1F - fraction) * start + fraction * end
    }

    private val tmpRect = Rect()
}

private fun clipOutPath(canvas: Canvas, path: Path) {
    if (Build.VERSION.SDK_INT >= 26) {
        CanvasVerificationHelper.clipOutPath(canvas, path)
    } else {
        @Suppress("DEPRECATION")
        canvas.clipPath(path, Region.Op.DIFFERENCE)
    }
}

@RequiresApi(26)
private object CanvasVerificationHelper {
    @DoNotInline
    fun clipOutPath(canvas: Canvas, path: Path) {
        canvas.clipOutPath(path)
    }
}