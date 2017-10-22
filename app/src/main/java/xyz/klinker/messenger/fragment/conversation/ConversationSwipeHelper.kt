package xyz.klinker.messenger.fragment.conversation

import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentActivity
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.View
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.fragment.ArchivedConversationListFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.util.SmsMmsUtils
import xyz.klinker.messenger.shared.util.SnackbarAnimationFix
import xyz.klinker.messenger.utils.swipe_to_dismiss.SwipeTouchHelper
import xyz.klinker.messenger.utils.swipe_to_dismiss.UnarchiveSwipeSimpleCallback
import java.util.ArrayList

class ConversationSwipeHelper(private val fragment: ConversationListFragment) {

    private val activity: FragmentActivity? by lazy { fragment.activity }

    private val pendingDelete = mutableListOf<Conversation>()
    private var pendingArchive = mutableListOf<Conversation>()

    private var deleteSnackbar: Snackbar? = null
    private var archiveSnackbar: Snackbar? = null

    fun getSwipeTouchHelper(adapter: ConversationListAdapter): ItemTouchHelper {
        return if (activity != null && fragment is ArchivedConversationListFragment)
            SwipeTouchHelper(UnarchiveSwipeSimpleCallback(adapter))
        else SwipeTouchHelper(adapter)
    }

    fun clearPending() {
        pendingDelete.clear()
        pendingArchive.clear()
    }

    fun onSwipeToDelete(conversation: Conversation) {
        pendingDelete.add(conversation)
        val currentSize = pendingDelete.size

        val plural = fragment.resources.getQuantityString(R.plurals.conversations_deleted,
                pendingDelete.size, pendingDelete.size)

        archiveSnackbar?.dismiss()

        deleteSnackbar = Snackbar.make(fragment.recyclerView, plural, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo) { fragment.recyclerManager.loadConversations() }
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(snackbar: Snackbar?, event: Int) {
                        super.onDismissed(snackbar, event)
                        dismissDeleteSnackbar(currentSize)
                        clearPending()
                    }
                })
        SnackbarAnimationFix.apply(deleteSnackbar!!)
        deleteSnackbar?.show()

        NotificationManagerCompat.from(activity).cancel(conversation.id.toInt())

        // for some reason, if this is done immediately then the final snackbar will not be
        // displayed
        Handler().postDelayed({ fragment.checkEmptyViewDisplay() }, 500)
    }

    fun onSwipeToArchive(conversation: Conversation) {
        pendingArchive.add(conversation)
        val currentSize = pendingArchive.size

        val plural = fragment.resources.getQuantityString(
                if (fragment is ArchivedConversationListFragment) R.plurals.conversations_unarchived else R.plurals.conversations_archived,
                pendingArchive.size, pendingArchive.size)

        deleteSnackbar?.dismiss()

        archiveSnackbar = Snackbar.make(fragment.recyclerView, plural, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo) { fragment.recyclerManager.loadConversations() }
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(snackbar: Snackbar?, event: Int) {
                        super.onDismissed(snackbar, event)
                        dismissArchiveSnackbar(currentSize)
                        clearPending()
                    }
                })
        SnackbarAnimationFix.apply(archiveSnackbar!!)
        archiveSnackbar?.show()

        NotificationManagerCompat.from(activity).cancel(conversation.id.toInt())

        // for some reason, if this is done immediately then the final snackbar will not be
        // displayed
        Handler().postDelayed({ fragment.checkEmptyViewDisplay() }, 500)
    }

    fun makeSnackbar(text: String, duration: Int, actionLabel: String?, listener: View.OnClickListener?) {
        val s = Snackbar.make(fragment.recyclerView, text, duration)

        if (actionLabel != null && listener != null) {
            s.setAction(actionLabel, listener)
        }

        SnackbarAnimationFix.apply(s)
        s.show()
    }

    fun dismissSnackbars() {
        archiveSnackbar?.dismiss()
        deleteSnackbar?.dismiss()

        dismissArchiveSnackbar(-1)
        dismissDeleteSnackbar(-1)
    }

    private fun dismissDeleteSnackbar(currentSize: Int) {
        deleteSnackbar = null

        val list = mutableListOf<Conversation>()
        list.addAll(pendingDelete)
        pendingDelete.clear()

        Thread {
            if ((currentSize == -1 || list.size == currentSize) && activity != null) {
                list.forEach { performDeleteOperation(it) }
            }
        }.start()
    }

    private fun dismissArchiveSnackbar(currentSize: Int) {
        archiveSnackbar = null

        val list = mutableListOf<Conversation>()
        list.addAll(pendingArchive)
        pendingArchive.clear()

        Thread {
            if ((currentSize == -1 || list.size == currentSize) && activity != null) {
                list.forEach { performArchiveOperation(it) }
            }
        }.start()
    }

    private fun performDeleteOperation(conversation: Conversation) {
        if (activity != null) {
            DataSource.deleteConversation(activity!!, conversation)
            SmsMmsUtils.deleteConversation(activity, conversation.phoneNumbers!!)
        }
    }

    private fun performArchiveOperation(conversation: Conversation) {
        if (activity != null) {
            if (fragment is ArchivedConversationListFragment) {
                DataSource.unarchiveConversation(activity!!, conversation.id)
            } else {
                DataSource.archiveConversation(activity!!, conversation.id)
            }
        }
    }
}