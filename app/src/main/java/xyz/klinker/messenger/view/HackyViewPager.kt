package xyz.klinker.messenger.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class HackyViewPager : ViewPager {

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    override fun onInterceptTouchEvent(ev: MotionEvent) = try {
        super.onInterceptTouchEvent(ev)
    } catch (e: IllegalArgumentException) {
        //uncomment if you really want to see these errors
        //e.printStackTrace();
        false
    }

}
