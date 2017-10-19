package xyz.klinker.messenger.fragment.conversation

import android.os.Handler
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import xyz.klinker.messenger.MessengerApplication
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.fragment.ArchivedConversationListFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.utils.FixedScrollLinearLayoutManager
import xyz.klinker.messenger.utils.swipe_to_dismiss.SwipeItemDecoration

class ConversationRecyclerViewManager(private val fragment: ConversationListFragment) {

    var adapter: ConversationListAdapter? = null
    private val layoutManager: FixedScrollLinearLayoutManager by lazy { FixedScrollLinearLayoutManager(fragment.activity) }

    val recyclerView: RecyclerView by lazy { fragment.rootView!!.findViewById<View>(R.id.recycler_view) as RecyclerView }
    private val empty: View by lazy { fragment.rootView!!.findViewById<View>(R.id.empty_view) }

    fun setupViews() {
        empty.setBackgroundColor(Settings.mainColorSet.colorLight)
        ColorUtils.changeRecyclerOverscrollColors(recyclerView, Settings.mainColorSet.color)
    }

    fun loadConversations() {
        fragment.swipeHelper.clearPending()

        val handler = Handler()
        Thread {
            val startTime = System.currentTimeMillis()

            if (fragment.activity == null) {
                return@Thread
            }

            val conversations = getCursorSafely()

            Log.v("conversation_load", "load took ${System.currentTimeMillis() - startTime} ms")

            if (fragment.activity == null) {
                return@Thread
            }

            handler.post {
                setConversations(conversations.toMutableList())
                fragment.lastRefreshTime = System.currentTimeMillis()

                try {
                    (fragment.activity.application as MessengerApplication).refreshDynamicShortcuts()
                } catch (e: Exception) {
                }
            }
        }.start()
    }

    fun canScroll(scrollable: Boolean) { layoutManager.setCanScroll(scrollable) }
    fun scrollToPosition(position: Int) { layoutManager.scrollToPosition(position) }
    fun getViewAtPosition(position: Int): View = recyclerView.findViewHolderForAdapterPosition(position).itemView

    private fun getCursorSafely() = when {
        fragment is ArchivedConversationListFragment && fragment.activity != null -> DataSource.getArchivedConversationsAsList(fragment.activity)
        fragment.activity != null -> DataSource.getUnarchivedConversationsAsList(fragment.activity)
        else -> emptyList()
    }

    private fun setConversations(conversations: MutableList<Conversation>) {
        if (fragment.activity == null) {
            return
        }

        if (adapter != null) {
            adapter!!.conversations = conversations
            adapter!!.notifyDataSetChanged()
        } else {
            adapter = ConversationListAdapter(fragment.activity as MessengerActivity,
                    conversations, fragment.multiSelector, fragment, fragment)

            layoutManager.setCanScroll(true)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter
            recyclerView.addItemDecoration(SwipeItemDecoration())

            val touchHelper = fragment.swipeHelper.getSwipeTouchHelper(adapter!!)
            touchHelper.attachToRecyclerView(recyclerView)
        }

        fragment.messageListManager.tryOpeningFromArguments()
        checkEmptyViewDisplay()
    }

    fun checkEmptyViewDisplay() {
        if (recyclerView.adapter.itemCount == 0 && empty.visibility == View.GONE) {
            empty.alpha = 0f
            empty.visibility = View.VISIBLE

            empty.animate().alpha(1f).setDuration(250).setListener(null)
        } else if (recyclerView.adapter.itemCount != 0 && empty.visibility == View.VISIBLE) {
            empty.visibility = View.GONE
        }
    }
}