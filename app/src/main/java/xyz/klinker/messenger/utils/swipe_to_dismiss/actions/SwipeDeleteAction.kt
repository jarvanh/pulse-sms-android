package xyz.klinker.messenger.utils.swipe_to_dismiss.actions

import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.shared.data.Settings

class SwipeDeleteAction : BaseSwipeAction() {

    override fun getIcon() = R.drawable.ic_delete_sweep
    override fun getBackgroundColor() = Settings.mainColorSet.colorAccent
    override fun onPerform(listener: ConversationListAdapter, index: Int) {
        listener.deleteItem(index)
    }

}