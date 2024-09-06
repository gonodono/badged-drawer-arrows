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
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.graphics.withTranslation
import kotlin.properties.Delegates.observable

class BadgedDrawerArrowDrawable(context: Context) :
    DrawerArrowDrawable(context) {

    var isBadgeEnabled: Boolean by invalidating(false, invalidateClip = true)

    sealed class BadgeSize(internal val get: BadgedDrawerArrowDrawable.() -> Float) {
        data object Standard : BadgeSize({ 3 * barThickness + 2 * gapSize })
        data object Dot : BadgeSize({ dotDiameter })
        data class Custom(val size: Float) : BadgeSize({ size })
    }

    var badgeSize: BadgeSize by changeable(BadgeSize.Standard) { size ->
        badgeDiameter = with(size) { get() }
        isClipInvalidated = true
        invalidateSelf()
    }

    @get:ColorInt
    @setparam:ColorInt
    var badgeColor: Int by invalidating(Color.RED)

    enum class Corner(internal val sx: Int, internal val sy: Int) {
        TopLeft(-1, -1), TopRight(1, -1), BottomRight(1, 1), BottomLeft(-1, 1)
    }

    var badgeCorner: Corner by invalidating(Corner.TopRight, true)

    var badgeOffset: PointF by invalidating(PointF(), true)

    var badgeClipMargin: Float by invalidating(0F, true)

    var badgeText: String? by calculating(null)

    var badgeTextSize: (default: Float) -> Float by calculating { it }

    @get:ColorInt
    @setparam:ColorInt
    var badgeTextColor: Int by invalidating(Color.WHITE)

    var badgeTextOffset: PointF by invalidating(PointF())

    sealed class Motion(
        internal val endScale: Float? = null,
        internal val endRotation: Float? = null
    ) {
        data object None : Motion()
        data object Grow : Motion(1.5F, null)
        data object Shrink : Motion(0F, null)
        data object FullSpinCW : Motion(null, 360F)
        data object FullSpinCCW : Motion(null, -360F)
        data object HalfSpinCW : Motion(null, 180F)
        data object HalfSpinCCW : Motion(null, -180F)

        operator fun plus(other: Motion): Motion = CombinedMotion(this, other)

        internal class CombinedMotion(
            private val first: Motion,
            private val second: Motion
        ) : Motion(
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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Motion) return false
            if (endScale != other.endScale) return false
            return endRotation == other.endRotation
        }

        override fun hashCode(): Int {
            var result = endScale?.hashCode() ?: 0
            result = 31 * result + (endRotation?.hashCode() ?: 0)
            return result
        }

        internal open val ss: String? get() = toString()
        internal open val rs: String? get() = toString()
    }

    var badgeMotion: Motion by changeable(Motion.None) {
        applyMotion(progress)
    }

    var autoMirrorOnReverse: Boolean = false

    var badgeDiameter: Float = with(badgeSize) { get() }
        private set

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
        val bx = (barLength / 2F) * badgeCorner.sx
        val centerX = bounds.centerX() + bx + badgeOffset.x
        val by = (1.5F * barThickness + gapSize) * badgeCorner.sy
        val centerY = bounds.centerY() + by + badgeOffset.y
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
        if (isBadgeEnabled) drawBadge(canvas, centerX, centerY, radius)
    }

    private var clipPath: Path? = null

    private var isClipInvalidated = false

    private fun calculateClipPath(
        centerX: Float,
        centerY: Float,
        radius: Float
    ): Path? {
        if (!isBadgeEnabled || badgeClipMargin <= 0F) {
            clipPath = null
            return null
        }

        val path = clipPath?.apply { if (!isClipInvalidated) return this }
            ?: Path().also { clipPath = it }

        path.rewind()
        val offsetY = centerY - bounds.top  // 'cause of the bug in super.
        val scaledRadius = (radius + badgeClipMargin) * scale
        path.addCircle(centerX, offsetY, scaledRadius, Path.Direction.CW)
        isClipInvalidated = false
        return path
    }

    private val textBounds = Rect()

    // A Bitmap cache for the text draw seems more efficient, but I've not
    // yet managed an acceptable result, due to the small text sizes, tiny draw
    // area, and the possible states. The text's measure is cached, at least.
    private fun calculateTextBounds(text: String?) {
        when {
            text.isNullOrBlank() -> textBounds.setEmpty()
            else -> {
                val default = badgeDiameter * textSizeFactor(text.length)
                paint.textSize = badgeTextSize(default)
                paint.getTextBounds(text, 0, text.length, textBounds)
            }
        }
        invalidateSelf()
    }

    private fun drawBadge(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {
        paint.color = badgeColor
        canvas.drawCircle(centerX, centerY, radius * scale, paint)

        if (badgeSize == BadgeSize.Dot) return
        val text = badgeText.takeIf { !it.isNullOrBlank() } ?: return

        paint.color = badgeTextColor
        val x = centerX - textBounds.exactCenterX() + badgeTextOffset.x
        val y = centerY - textBounds.exactCenterY() + badgeTextOffset.y

        canvas.save()
        canvas.rotate(rotation, centerX, centerY)
        canvas.scale(scale, scale, centerX, centerY)
        canvas.drawText(text, x, y, paint)
        canvas.restore()
    }

    private var scale = 1F

    private var rotation = 0F

    private var verticalMirror = false

    override fun setProgress(progress: Float) {
        if (autoMirrorOnReverse) when (progress) {
            1F -> setVerticalMirror(true)
            0F -> setVerticalMirror(false)
        }
        if (this.progress != progress) applyMotion(progress)
        super.setProgress(progress)
    }

    override fun setVerticalMirror(verticalMirror: Boolean) {
        super.setVerticalMirror(verticalMirror)
        this.verticalMirror = verticalMirror
    }

    private fun applyMotion(progress: Float) {
        val newScale = when (val end = badgeMotion.endScale) {
            null -> 1F
            else -> lerp(1F, end, progress)
        }
        val newRotation = when (val end = badgeMotion.endRotation) {
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

        // Fraction of the badge's diameter that is used for the default size.
        // This really only handles lengths of 1, 2, or 3. Tweak as needed.
        @FloatRange(0.0, 1.0)
        private fun textSizeFactor(textLength: Int): Float =
            when (textLength) {
                1 -> 0.75F
                2 -> 0.6F
                else -> 0.5F
            }
    }

    // This is figured at init so that we don't have to hang on the the Context.
    private val dotDiameter = DOT_DP * context.resources.displayMetrics.density

    private fun <T> invalidating(initial: T, invalidateClip: Boolean = false) =
        changeable(initial) {
            if (invalidateClip) isClipInvalidated = true
            invalidateSelf()
        }

    private fun <T> calculating(initial: T) =
        changeable(initial) { calculateTextBounds(badgeText) }
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

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    (1F - fraction) * start + fraction * end

private inline fun <T> changeable(
    initial: T,
    crossinline onChange: (T) -> Unit
) = observable(initial) { _, old, new -> if (old != new) onChange(new) }