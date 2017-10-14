package xyz.klinker.messenger.shared.util.listener

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.view.MotionEvent
import android.view.View

class ForcedRippleTouchListener(private val rippleView: View) : View.OnTouchListener {

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            forceRippleAnimation(rippleView, event)
        }

        return false
    }

    private fun forceRippleAnimation(view: View, event: MotionEvent) {
        val background = (view.parent as View).background

        if (background is RippleDrawable) {
            background.state = intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled)
            background.setHotspot(event.x, event.y)

            view.postDelayed({ background.state = intArrayOf() }, RIPPLE_TIMEOUT_MS)
        }
    }

    companion object {
        private val RIPPLE_TIMEOUT_MS: Long = 50
    }
}
