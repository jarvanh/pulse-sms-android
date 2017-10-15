package xyz.klinker.messenger.utils.swipe_to_dismiss

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable

import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.ConversationListAdapter
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.ColorUtils

@Suppress("DEPRECATION")
class SwipeDeleteSimpleCallback(adapter: ConversationListAdapter) : SwipeSimpleCallback(adapter) {

    override fun setupEndSwipe(context: Context) {
        endSwipeBackground = ColorDrawable(Settings.mainColorSet.colorAccent)
        endMark = context.getDrawable(R.drawable.ic_delete_sweep)

        if (ColorUtils.isColorDark(Settings.mainColorSet.colorAccent)) {
            endMark?.setTintList(ColorStateList.valueOf(context.resources.getColor(R.color.deleteIcon)))
        } else {
            endMark?.setTintList(ColorStateList.valueOf(context.resources.getColor(R.color.lightToolbarTextColor)))
        }
    }
}
