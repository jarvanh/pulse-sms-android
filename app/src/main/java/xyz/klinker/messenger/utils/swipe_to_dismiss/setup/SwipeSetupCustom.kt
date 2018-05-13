package xyz.klinker.messenger.utils.swipe_to_dismiss.setup

import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.SwipeOption
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.BaseSwipeAction
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.SwipeArchiveAction
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.SwipeDeleteAction
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.SwipeNoAction

class SwipeSetupCustom(adapter: ConversationListAdapter) : SwipeSetupBase(adapter) {

    override fun getLeftToRightAction() = mapToAction(Settings.leftToRightSwipe)
    override fun getRightToLeftAction() = mapToAction(Settings.rightToLeftSwipe)

    private fun mapToAction(option: SwipeOption): BaseSwipeAction {
        return when (option) {
            SwipeOption.ARCHIVE -> SwipeArchiveAction()
            SwipeOption.DELETE -> SwipeDeleteAction()
            SwipeOption.NONE -> SwipeNoAction()
        }
    }

}