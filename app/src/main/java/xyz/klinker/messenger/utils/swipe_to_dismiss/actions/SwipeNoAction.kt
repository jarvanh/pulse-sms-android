package xyz.klinker.messenger.utils.swipe_to_dismiss.actions

import android.graphics.Color
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter

class SwipeNoAction : BaseSwipeAction() {

    override fun getIcon() = R.drawable.ic_back
    override fun getBackgroundColor() = Color.TRANSPARENT
    override fun onPerform(listener: ConversationListAdapter, index: Int) { }

}