package xyz.klinker.messenger.utils.swipe_to_dismiss.setup

import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.BaseSwipeAction
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.SwipeDeleteAction
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.SwipeUnarchiveAction

@Suppress("DEPRECATION")
class SwipeSetupUnarchive(adapter: ConversationListAdapter) : SwipeSetupBase(adapter) {

    override fun getLeftToRightAction(): BaseSwipeAction {
        return SwipeUnarchiveAction()
    }

    override fun getRightToLeftAction(): BaseSwipeAction {
        return SwipeDeleteAction()
    }

}
