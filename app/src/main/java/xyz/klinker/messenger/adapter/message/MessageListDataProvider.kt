package xyz.klinker.messenger.adapter.message

import android.content.Context
import android.database.Cursor
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.fragment.MessageListFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.CursorUtil
import xyz.klinker.messenger.shared.util.DensityUtil
import xyz.klinker.messenger.shared.util.SnackbarAnimationFix

class MessageListDataProvider(private val adapter: MessageListAdapter, private val fragment: MessageListFragment,
                              initialCursor: Cursor) {

    var messages = initialCursor

    fun addMessage(recycler: RecyclerView, newMessages: Cursor) {
        val initialCount = adapter.itemCount

        CursorUtil.closeSilent(messages)
        messages = newMessages

        val finalCount = adapter.itemCount

        if (initialCount == finalCount) {
            adapter.notifyItemChanged(finalCount - 1)
        } else if (initialCount > finalCount) {
            adapter.notifyDataSetChanged()
        } else {
            if (finalCount - 2 >= 0) {
                // with the new paddings, we need to notify the second to last item too
                adapter.notifyItemChanged(finalCount - 2)
            }

            adapter.notifyItemInserted(finalCount - 1)

            if (Math.abs((recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition() - initialCount) < 4) {
                // near the bottom, scroll to the new item
                recycler.layoutManager.scrollToPosition(finalCount - 1)
            } else if (messages.moveToLast()) {
                val message = Message()
                message.fillFromCursor(messages)
                if (message.type == Message.TYPE_RECEIVED) {
                    val text = recycler.context.getString(R.string.new_message)
                    adapter.snackbar = Snackbar
                            .make(recycler, text, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.read) { recycler.layoutManager.scrollToPosition(finalCount - 1) }

                    try {
                        (adapter.snackbar!!.view.layoutParams as CoordinatorLayout.LayoutParams)
                                .bottomMargin = DensityUtil.toDp(recycler.context, 56)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    SnackbarAnimationFix.apply(adapter.snackbar!!)
                    adapter.snackbar!!.show()
                }
            }
        }
    }

    fun onMessageDeleted(context: Context, conversationId: Long, position: Int) {
        val source = DataSource

        val messageList = source.getMessages(context, conversationId, 1)
        if (messageList.isEmpty()) {
            (fragment.activity as MessengerActivity).navController.drawerItemClicked(R.id.menu_delete_conversation)
        } else {
            val message = messageList[0]

            val conversation = source.getConversation(context, conversationId)
            source.updateConversation(context, conversationId, true, message.timestamp,
                    if (message.type == Message.TYPE_SENT || message.type == Message.TYPE_SENDING)
                        context.getString(R.string.you) + ": " + message.data else message.data,
                    message.mimeType, conversation != null && conversation.archive)

            fragment.setConversationUpdateInfo(if (message.type == Message.TYPE_SENDING)
                context.getString(R.string.you) + ": " + message.data else message.data!!)
        }
    }
}