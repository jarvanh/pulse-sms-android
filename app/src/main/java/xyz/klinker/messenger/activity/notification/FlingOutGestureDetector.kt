package xyz.klinker.messenger.activity.notification

import android.view.GestureDetector
import android.view.MotionEvent

class FlingOutGestureDetector(private val activity: MarshmallowReplyActivity) : GestureDetector.SimpleOnGestureListener() {

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        if (velocityY < -3000 && velocityX < 7000 && velocityX > -7000) {
            activity.onBackPressed()
        }
        return false
    }
}