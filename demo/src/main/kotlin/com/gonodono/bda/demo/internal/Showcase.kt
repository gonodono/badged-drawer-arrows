package com.gonodono.bda.demo.internal

import android.R.attr.textColorPrimary
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import com.google.android.material.R.attr.colorSurface
import kotlin.math.sqrt

class Showcase(
    private val activity: Activity,
    private val targetView: View,
    private val marginDp: Int,
    private val label: String,
    private val onDisposed: () -> Unit
) : ColorDrawable() {

    companion object {

        fun checkShow(activity: Activity): Boolean =
            activity.getPreferences(MODE_PRIVATE)
                .getBoolean(PREF_SHOW_SHOWCASE, true)
    }

    private val attrs =
        intArrayOf(textColorPrimary, colorSurface)
            .sorted().toTypedArray().toIntArray()

    private val textPaint = Paint().apply {
        setShadowLayer(3F, 0F, 0F, Color.CYAN)
        textAlign = Paint.Align.CENTER
    }

    init {
        val a = activity.obtainStyledAttributes(attrs)

        val bgIndex = attrs.indexOf(textColorPrimary)
        val bg = a.getColorStateList(bgIndex)?.defaultColor ?: Color.LTGRAY
        color = ColorUtils.setAlphaComponent(bg, 230)

        textPaint.apply {
            val textIndex = attrs.indexOf(colorSurface)
            color = a.getColorStateList(textIndex)?.defaultColor ?: Color.DKGRAY
            typeface = Typeface.DEFAULT_BOLD
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                36F,
                activity.resources.displayMetrics
            )
        }

        a.recycle()

        activity.window.decorView.foreground = this
    }

    override fun draw(canvas: Canvas) {
        val rect = RectF()

        val view = targetView
        val width = view.width * view.scaleX
        val height = view.height * view.scaleY
        val diameter = sqrt(width * width + height * height) +
                marginDp * activity.resources.displayMetrics.density
        rect.set(0F, 0F, diameter, diameter)

        val location = IntArray(2)
        view.getLocationInWindow(location)

        val dx = location[0].toFloat() - (diameter - width) / 2
        val dy = location[1].toFloat() - (diameter - height) / 2
        rect.offset(dx, dy)

        val path = Path().apply { addOval(rect, Path.Direction.CW) }

        val count = canvas.save()
        canvas.clipOutPath(path)
        super.draw(canvas)
        canvas.restoreToCount(count)

        canvas.drawText(
            label,
            rect.centerX(),
            rect.bottom + 1.5F * textPaint.textSize,
            textPaint
        )
    }

    fun dispose() {
        ValueAnimator.ofInt(color.alpha, 0).run {
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        activity.apply {
                            window.decorView.foreground = null
                            getPreferences(MODE_PRIVATE).edit {
                                putBoolean(PREF_SHOW_SHOWCASE, false)
                            }
                        }
                        onDisposed()
                    }
                }
            )
            addUpdateListener {
                val alpha = animatedValue as Int
                color = ColorUtils.setAlphaComponent(color, alpha)
            }
            duration = 150
            start()
        }
    }
}

private const val PREF_SHOW_SHOWCASE = "show_showcase"