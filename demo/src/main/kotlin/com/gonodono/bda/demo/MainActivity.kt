package com.gonodono.bda.demo

import android.R.attr.colorControlHighlight
import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.TouchDelegate
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.compose.ui.unit.Dp
import androidx.core.widget.doOnTextChanged
import com.gonodono.bda.compose.BadgedDrawerArrow
import com.gonodono.bda.demo.databinding.ActivityMainBinding
import com.gonodono.bda.demo.internal.DividersDrawable
import com.gonodono.bda.demo.internal.SelectedListener
import com.gonodono.bda.demo.internal.getThemeColor
import com.gonodono.bda.demo.internal.setSwatchColor
import com.gonodono.bda.demo.internal.showColorDialog
import com.gonodono.bda.view.BadgedDrawerArrowDrawable
import androidx.compose.ui.graphics.Color as ComposeColor
import com.gonodono.bda.compose.BadgeSize as ComposeBadgeSize
import com.gonodono.bda.compose.Corner as ComposeCorner
import com.gonodono.bda.compose.Motion as ComposeMotion
import com.gonodono.bda.view.BadgedDrawerArrowDrawable.BadgeSize as DrawableBadgeSize
import com.gonodono.bda.view.BadgedDrawerArrowDrawable.Corner as DrawableCorner
import com.gonodono.bda.view.BadgedDrawerArrowDrawable.Motion as DrawableMotion

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        val drawable = BadgedDrawerArrowDrawable(this).apply {
            isBadgeEnabled = true
            badgeClipMargin = 6F
            badgeText = "99+"
            // Causes it to mimic ActionBarDrawerToggle's anim w/o being in one.
            autoMirrorOnReverse = true
        }

        var progress by mutableFloatStateOf(drawable.progress)
        var isBadgeEnabled by mutableStateOf(drawable.isBadgeEnabled)
        var badgeSize by mutableStateOf(drawable.badgeSize.toComposeBadgeSize())
        var badgeColor by mutableStateOf(drawable.badgeColor.toComposeColor())
        var badgeCorner by mutableStateOf(drawable.badgeCorner.toComposeCorner())
        var badgeOffset by mutableStateOf(drawable.badgeOffset.toComposeOffset())
        var badgeClipMargin by mutableStateOf(drawable.badgeClipMargin.toDp(this))
        var badgeText by mutableStateOf(drawable.badgeText)
        var badgeTextColor by mutableStateOf(drawable.badgeTextColor.toComposeColor())
        var badgeMotion by mutableStateOf(drawable.badgeMotion.toComposeMotion())
        var scaleState by mutableFloatStateOf(1F)

        ui.root.foreground = DividersDrawable(ui.root)

        // Setup is grouped and ordered the same as the layout, top to bottom.

        ui.groupFramework.setOnCheckedChangeListener { _, checkedId ->
            ui.dualPane.displayedChild = when (checkedId) {
                ui.radioCompose.id -> ui.dualPane.indexOfChild(ui.composeView)
                else -> ui.dualPane.indexOfChild(ui.viewContainer)
            }
        }

        ui.isBadgeEnabled.setOnCheckedChangeListener { _, isChecked ->
            drawable.isBadgeEnabled = isChecked
            isBadgeEnabled = isChecked
            ui.isDot.isEnabled = isChecked
            ui.labelIsDot.isEnabled = isChecked
        }
        ui.isDot.setOnCheckedChangeListener { _, isChecked ->
            drawable.badgeSize = when {
                isChecked -> DrawableBadgeSize.Dot
                else -> DrawableBadgeSize.Standard
            }
            badgeSize = when {
                isChecked -> ComposeBadgeSize.Dot
                else -> ComposeBadgeSize.Standard
            }
        }
        ui.badgeText.doOnTextChanged { text, _, _, _ ->
            drawable.badgeText = text.toString()
            badgeText = text.toString()
        }

        // Cheap and easy animation. Not concerned with correctness.
        val animator = ValueAnimator()
        animator.addUpdateListener {
            ui.progress.value = it.animatedValue as Float
        }
        fun handleClick() {
            animator.cancel()
            when (drawable.progress) {
                0F -> animator.setObjectValues(0F, 1F)
                else -> animator.setObjectValues(1F, 0F)
            }
            animator.start()
        }
        ui.view.apply {
            setOnClickListener { handleClick() }
            // The overlay doesn't mess with bounds, like e.g. the background.
            overlay.add(drawable)
        }

        ui.progress.addOnChangeListener { _, value, _ ->
            drawable.progress = value
            progress = value
        }

        // badgeOffset is for tweaking the badge placement, if it's not exactly
        // where you'd like it. It is a raw offset, to keep things simple, and
        // it's up to the user to figure out the appropriate values.
        fun updateOffset() {
            val value = ui.badgeOffset.value
            val offset = when (drawable.badgeCorner) {
                DrawableCorner.TopLeft -> PointF(-value, -value)
                DrawableCorner.TopRight -> PointF(value, -value)
                DrawableCorner.BottomRight -> PointF(value, value)
                DrawableCorner.BottomLeft -> PointF(-value, value)
            }
            drawable.badgeOffset = offset
            badgeOffset = offset.toComposeOffset()
        }
        ui.badgeCorner.apply {
            setLabelFormatter { value ->
                DrawableCorner.entries
                    .getOrNull(value.toInt())?.toString() ?: ""
            }
            value = drawable.badgeCorner.ordinal.toFloat()
            addOnChangeListener { _, value, _ ->
                drawable.badgeCorner = DrawableCorner.entries[value.toInt()]
                badgeCorner = ComposeCorner.entries[value.toInt()]
                updateOffset()
            }
        }
        ui.badgeOffset.addOnChangeListener { _, _, _ -> updateOffset() }

        ui.badgeClipMargin.apply {
            addOnChangeListener { _, value, _ ->
                drawable.badgeClipMargin = value
                badgeClipMargin = value.toDp(context)
            }
        }

        ui.badgeMotion.apply {
            adapter = ArrayAdapter(context, simple_spinner_item, Motions)
                .apply { setDropDownViewResource(simple_spinner_dropdown_item) }
            onItemSelectedListener = SelectedListener { position ->
                drawable.badgeMotion = Motions[position]
                badgeMotion = ComposeMotions[position]
            }
            setSelection(Motions.lastIndex)
        }

        ui.badgeColor.apply {
            setSwatchColor(drawable.badgeColor)
            setOnClickListener {
                showColorDialog(drawable.badgeColor) { color ->
                    drawable.badgeColor = color
                    badgeColor = color.toComposeColor()
                    setSwatchColor(color)
                }
            }
        }
        ui.badgeTextColor.apply {
            setSwatchColor(drawable.badgeTextColor)
            setOnClickListener {
                showColorDialog(drawable.badgeTextColor) { color ->
                    drawable.badgeTextColor = color
                    badgeTextColor = color.toComposeColor()
                    setSwatchColor(color)
                }
            }
        }

        // Same as Compose's minimumInteractiveComponentSize().
        val elementSize = 48 * resources.displayMetrics.density

        ui.dualPane.addOnLayoutChangeListener { _, l, t, r, b, _, _, _, _ ->
            // Centering the drawable in the overlay.
            drawable.apply {
                val x = (ui.view.width - intrinsicWidth) / 2
                val y = (ui.view.height - intrinsicHeight) / 2
                setBounds(x, y, x + intrinsicWidth, y + intrinsicHeight)
                invalidateSelf()
            }
            // DrawerArrowDrawable does not scale to its bounds, so both the
            // View and the Composable are scaled up to show the details.
            val maxWidth = r - l
            val maxHeight = b - t
            val scale = 0.6F * when {
                maxWidth > maxHeight -> maxHeight / elementSize
                else -> maxWidth / elementSize
            }
            scaleState = scale
            ui.view.scaleX = scale; ui.view.scaleY = scale
            val scaledSize = (elementSize * scale).toInt()
            val offset = ((scaledSize - elementSize) / 2F).toInt()
            val bounds = Rect(0, 0, scaledSize, scaledSize)
            bounds.offset(-offset, -offset)
            ui.viewContainer.touchDelegate = TouchDelegate(bounds, ui.view)
        }

        val barColor = drawable.color.toComposeColor()
        val background = (ui.view.background as? ColorDrawable)?.color
            ?: getThemeColor(colorControlHighlight, Color.LTGRAY)
        val backgroundColor = background.toComposeColor()

        ui.composeView.apply {
            setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    BadgedDrawerArrow(
                        barColor = barColor,
                        progress = progress,
                        isBadgeEnabled = isBadgeEnabled,
                        badgeSize = badgeSize,
                        badgeColor = badgeColor,
                        badgeCorner = badgeCorner,
                        badgeOffset = badgeOffset,
                        badgeClipMargin = badgeClipMargin,
                        badgeText = badgeText,
                        badgeTextColor = badgeTextColor,
                        badgeMotion = badgeMotion,
                        autoMirrorOnReverse = true,
                        onClick = ::handleClick,
                        modifier = Modifier
                            .scale(scaleState)
                            .background(backgroundColor)
                    )
                }
            }
        }
    }
}

private fun DrawableBadgeSize.toComposeBadgeSize() = when (this) {
    is DrawableBadgeSize.Dot -> ComposeBadgeSize.Dot
    else -> ComposeBadgeSize.Standard
}

private fun Int.toComposeColor() = ComposeColor(this)

private fun PointF.toComposeOffset() = Offset(x, y)

private fun DrawableMotion.toComposeMotion() = ComposeMotions
    .getOrElse(Motions.indexOf(this)) { ComposeMotion.None }

private fun BadgedDrawerArrowDrawable.Corner.toComposeCorner() = when (this) {
    DrawableCorner.TopLeft -> ComposeCorner.TopLeft
    DrawableCorner.TopRight -> ComposeCorner.TopRight
    DrawableCorner.BottomRight -> ComposeCorner.BottomRight
    DrawableCorner.BottomLeft -> ComposeCorner.BottomLeft
}

private fun Float.toDp(context: Context) =
    Dp(this / context.resources.displayMetrics.density)

private val Motions = listOf(
    DrawableMotion.None,
    DrawableMotion.Grow,
    DrawableMotion.Shrink,
    DrawableMotion.FullSpinCW,
    DrawableMotion.FullSpinCCW,
    DrawableMotion.HalfSpinCW,
    DrawableMotion.HalfSpinCCW,
    DrawableMotion.Grow + DrawableMotion.FullSpinCW,
    DrawableMotion.Shrink + DrawableMotion.HalfSpinCCW
)

private val ComposeMotions = listOf(
    ComposeMotion.None,
    ComposeMotion.Grow,
    ComposeMotion.Shrink,
    ComposeMotion.FullSpinCW,
    ComposeMotion.FullSpinCCW,
    ComposeMotion.HalfSpinCW,
    ComposeMotion.HalfSpinCCW,
    ComposeMotion.Grow + ComposeMotion.FullSpinCW,
    ComposeMotion.Shrink + ComposeMotion.HalfSpinCCW
)