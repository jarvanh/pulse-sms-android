package xyz.klinker.messenger.shared.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import xyz.klinker.messenger.shared.R

class MaxHeightScrollView : FrameLayout {

    private var maxHeight: Int = 0

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        if (!isInEditMode) {
            init(context, attrs)
        }
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        if (!isInEditMode) {
            init(context, attrs)
        }
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.max_height_scroll_view)
            maxHeight = styledAttrs.getDimensionPixelSize(R.styleable.max_height_scroll_view_max_height, DEFAULT_MAX_DP)

            styledAttrs.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMeasureSpec = heightMeasureSpec
        heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    companion object {
        private val DEFAULT_MAX_DP = 100
    }
}
