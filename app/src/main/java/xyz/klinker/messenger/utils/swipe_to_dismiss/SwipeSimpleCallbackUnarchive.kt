package xyz.klinker.messenger.utils.swipe_to_dismiss

import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.BaseSwipeAction
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.SwipeDeleteAction
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.SwipeUnarchiveAction

@Suppress("DEPRECATION")
class SwipeSimpleCallbackUnarchive(adapter: ConversationListAdapter) : SwipeSimpleCallbackBase(adapter) {

    override fun getStartSwipeAction(): BaseSwipeAction {
        return SwipeUnarchiveAction()
    }

    override fun getEndSwipeAction(): BaseSwipeAction {
        return SwipeDeleteAction()
    }

}
