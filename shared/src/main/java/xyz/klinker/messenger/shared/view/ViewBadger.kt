package xyz.klinker.messenger.shared.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewParent
import android.widget.FrameLayout
import android.widget.TextView

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.util.DensityUtil

@SuppressLint("AppCompatCustomView", "ViewConstructor")
class ViewBadger(context: Context, target: View) : TextView(context, null, android.R.attr.textViewStyle) {

    private val defaultBackground: ShapeDrawable
        get() {
            val r = DensityUtil.toDp(context, CORNER_RADIUS)
            val outerR = floatArrayOf(r.toFloat(), r.toFloat(), r.toFloat(), r.toFloat(), r.toFloat(), r.toFloat(), r.toFloat(), r.toFloat())

            val rr = RoundRectShape(outerR, null, null)
            val drawable = ShapeDrawable(rr)
            drawable.paint.color = context.resources.getColor(R.color.secondaryText)

            return drawable
        }

    init {
        init(context, target)
    }

    private fun init(context: Context, target: View) {
        val paddingPixels = DensityUtil.toDp(context, 1)

        textSize = 10f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(paddingPixels, 0, paddingPixels, 0)

        applyTo(target)
        show()
    }

    private fun applyTo(target: View) {
        val lp = target.layoutParams
        val parent = target.parent
        val container = FrameLayout(context!!)

        val group = parent as ViewGroup
        val index = group.indexOfChild(target)

        group.removeView(target)
        group.addView(container, index, lp)

        container.addView(target)
        container.addView(this)
        group.invalidate()
    }

    private fun show() {
        if (background == null) {
            setBackgroundDrawable(defaultBackground)
            setTextColor(context.resources.getColor(R.color.background))
        }

        applyLayoutParams()

        this.visibility = View.VISIBLE
    }

    private fun applyLayoutParams() {
        val lp = FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.START or Gravity.TOP
        lp.setMargins(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT, 0)

        layoutParams = lp
    }

    companion object {
        private val CORNER_RADIUS = 2
        private val MARGIN_LEFT = 14
        private val MARGIN_TOP = 14
    }
}
