package xyz.klinker.messenger.shared.view

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import androidx.drawerlayout.widget.DrawerLayout
import xyz.klinker.messenger.shared.util.DensityUtil

/**
 * This is used for Android Q, to define the exclusion rectangles, for the gestures. Once the newer
 * drawer layout comes out of Android X, we should be able to use it and remove this one.
 */
class ExclusionRectDrawerLayout : DrawerLayout {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    private val exclusionRects: List<Rect>

    init {
        exclusionRects = mutableListOf()
        exclusionRects.add(Rect(0, 0, DensityUtil.toDp(context, 100), 0))
    }


    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exclusionRects[0].bottom = b
            systemGestureExclusionRects = exclusionRects
        }

        super.onLayout(changed, l, t, r, b)
    }

}