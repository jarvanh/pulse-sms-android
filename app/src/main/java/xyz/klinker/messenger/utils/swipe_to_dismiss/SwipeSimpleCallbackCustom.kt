package xyz.klinker.messenger.utils.swipe_to_dismiss

import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.BaseSwipeAction
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.SwipeArchiveAction
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.SwipeDeleteAction

class SwipeSimpleCallbackCustom(adapter: ConversationListAdapter) : SwipeSimpleCallbackBase(adapter) {

    override fun getLeftToRightAction(): BaseSwipeAction {
        return SwipeArchiveAction()
    }

    override fun getRightToLeftAction(): BaseSwipeAction {
        return if (Settings.swipeDelete) SwipeDeleteAction() else SwipeArchiveAction()
    }

}