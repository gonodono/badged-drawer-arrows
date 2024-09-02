package com.gonodono.bda.demo.internal

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.PaintDrawable
import android.view.View
import androidx.appcompat.R.attr.colorControlNormal
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.forEach

// Bespoke decorator for the main content ViewGroup.
internal class DividersDrawable(private val cl: ConstraintLayout) :
    PaintDrawable() {

    private val viewPairs = mutableListOf<Pair<View, View>>()

    init {
        viewPairs.clear()
        cl.forEach { view ->
            view.tag.takeIf { it == "divider" } ?: return@forEach
            val params = view.layoutParams as? ConstraintLayout.LayoutParams
            params ?: return@forEach
            val other = cl.findViewById<View>(params.bottomToTop)
            other ?: return@forEach
            viewPairs += view to other
        }
        paint.style = Paint.Style.STROKE
        paint.color = cl.context.getThemeColor(colorControlNormal, Color.GRAY)
    }

    override fun draw(canvas: Canvas) {
        viewPairs.forEach { (view1, view2) ->
            val y = (view1.bottom + view2.top) / 2F
            canvas.drawLine(
                0.15F * bounds.width(), y,
                0.85F * bounds.width(), y,
                paint
            )
        }
    }
}