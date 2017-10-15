package xyz.klinker.messenger.utils.swipe_to_dismiss

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.ConversationListAdapter
import xyz.klinker.messenger.shared.data.Settings

@Suppress("DEPRECATION")
class UnarchiveSwipeSimpleCallback(adapter: ConversationListAdapter) : SwipeSimpleCallback(adapter) {

    override fun getArchiveItem(context: Context): Drawable? {
        return context.getDrawable(R.drawable.ic_unarchive)
    }

    override fun setupEndSwipe(context: Context) {
        endSwipeBackground = ColorDrawable(Settings.mainColorSet.colorAccent)
        endMark = context.getDrawable(R.drawable.ic_delete_sweep)
        endMark?.setColorFilter(context.resources.getColor(R.color.deleteIcon), PorterDuff.Mode.SRC_ATOP)
    }
}
