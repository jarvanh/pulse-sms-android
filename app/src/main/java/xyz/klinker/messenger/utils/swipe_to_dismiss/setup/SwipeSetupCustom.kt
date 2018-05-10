package xyz.klinker.messenger.utils.swipe_to_dismiss.setup

import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.BaseSwipeAction
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.SwipeArchiveAction
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.SwipeDeleteAction

class SwipeSetupCustom(adapter: ConversationListAdapter) : SwipeSetupBase(adapter) {

    override fun getLeftToRightAction(): BaseSwipeAction {
        return SwipeArchiveAction()
    }

    override fun getRightToLeftAction(): BaseSwipeAction {
        return if (Settings.swipeDelete) SwipeDeleteAction() else SwipeArchiveAction()
    }

}