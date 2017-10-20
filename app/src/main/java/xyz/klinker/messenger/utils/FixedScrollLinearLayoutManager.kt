package xyz.klinker.messenger.utils

import android.content.Context
import android.support.v7.widget.LinearLayoutManager

class FixedScrollLinearLayoutManager(context: Context?) : LinearLayoutManager(context) {

    private var canScroll = true

    fun setCanScroll(canScroll: Boolean) {
        this.canScroll = canScroll
    }

    override fun canScrollVertically(): Boolean {
        return canScroll && super.canScrollVertically()
    }
}
