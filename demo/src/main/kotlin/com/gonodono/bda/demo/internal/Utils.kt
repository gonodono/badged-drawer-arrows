package com.gonodono.bda.demo.internal

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import com.gonodono.bda.demo.R

@SuppressLint("DefaultLocale")
internal fun Context.showColorDialog(initial: Int, onComplete: (Int) -> Unit) {
    val fields = Color::class.java.fields.sortedBy { it.name }
    val values = fields.map { it.get(null) as Int }.toTypedArray()
    @Suppress("DEPRECATION") val names =
        fields.map { it.name.lowercase().capitalize() }
    AlertDialog.Builder(this@showColorDialog)
        .setSingleChoiceItems(
            names.toTypedArray<String>(),
            values.indexOf(initial)
        ) { dialog, which ->
            onComplete(values[which])
            dialog.dismiss()
        }
        .show()
}

@ColorInt
internal fun Context.getThemeColor(
    @AttrRes attr: Int,
    @ColorInt default: Int
): Int = obtainStyledAttributes(intArrayOf(attr)).run {
    try {
        getColor(0, default)
    } finally {
        recycle()
    }
}

internal fun Button.setSwatchColor(color: Int) {
    if (compoundDrawablesRelative[2] !is LayerDrawable) {
        setCompoundDrawablesRelativeWithIntrinsicBounds(
            0, 0, R.drawable.color_swatch, 0
        )
    }
    (compoundDrawablesRelative[2] as? LayerDrawable)?.run {
        findDrawableByLayerId(R.id.swatch)?.setTint(color)
    }
}

internal fun interface SelectedListener : AdapterView.OnItemSelectedListener {

    fun onSelected(position: Int)

    override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ) {
        onSelected(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}
}