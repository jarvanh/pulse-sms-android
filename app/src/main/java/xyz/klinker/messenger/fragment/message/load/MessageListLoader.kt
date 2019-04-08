package xyz.klinker.messenger.fragment.message.load

import android.database.Cursor
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestion
import com.l4digital.fastscroll.FastScrollRecyclerView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.message.MessageListAdapter
import xyz.klinker.messenger.fragment.message.MessageListFragment
import xyz.klinker.messenger.fragment.message.ConversationInformationUpdater
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Contact
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.service.notification.NotificationConstants
import xyz.klinker.messenger.shared.util.*
import java.util.*

class MessageListLoader(private val fragment: MessageListFragment) {
    
    val informationUpdater = ConversationInformationUpdater(fragment)
    private val activity: FragmentActivity? by lazy { fragment.activity }

    private val argManager
        get() = fragment.argManager
    private val draftManager
        get() = fragment.draftManager
    private val smartReplyManager
        get() = fragment.smartReplyManager

    private val manager: LinearLayoutManager by lazy { LinearLayoutManager(activity) }
    val messageList: FastScrollRecyclerView by lazy { fragment.rootView!!.findViewById<View>(R.id.message_list) as FastScrollRecyclerView }
    
    var adapter: MessageListAdapter? = null

    private var contactMap: Map<String, Contact>? = null
    private var contactByNameMap: Map<String, Contact>? = null
    
    private val listRefreshMonitor = MessageListRefreshMonitor()
    private var limitMessagesBasedOnPreviousSize = true
    var messageLoadedCount = -1
    
    fun initRecycler() {
        ColorUtils.changeRecyclerOverscrollColors(messageList, argManager.color)

        manager.stackFromEnd = true
        messageList.layoutManager = manager

        val color = if (Settings.useGlobalThemeColor) Settings.mainColorSet.color else argManager.color
        messageList.setBubbleColor(color)
        messageList.setHandleColor(color)

        messageList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val visibleItemCount = manager.childCount
                val totalItemCount = manager.itemCount
                val pastVisibleItems = manager.findFirstVisibleItemPosition()
                if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                    adapter?.snackbar?.dismiss()
                }
            }
        })
    }

    fun loadMessages(addedNewMessage: Boolean = false) {
        val handler = Handler()
        Thread {
            PerformanceProfiler.logEvent("loading messages")

            if (activity == null) {
                return@Thread
            }

            try {
                listRefreshMonitor.incrementRefreshThreadsCount()
                draftManager.loadDrafts()

                val cursor: Cursor
                if (argManager.limitMessages && argManager.messageToOpen == -1L && limitMessagesBasedOnPreviousSize) {
                    // weird logic with the counts for this. If we just load the MESSAGE_LIMIT each time,
                    // then the adapter gets screwed up and can display the wrong messages, since recycler views
                    // are meant to be "smart" about managing state.
                    // So, if we send a message, or a message is received, we should increment the number of messages
                    // that we are reading from the database, to account for this.

                    cursor = DataSource.getMessageCursorWithLimit(activity!!, argManager.conversationId,
                            when {
                                messageLoadedCount == -1 -> MESSAGE_LIMIT
                                addedNewMessage -> messageLoadedCount + 1
                                else -> messageLoadedCount
                            }
                    )

                    if (cursor.count < MESSAGE_LIMIT) {
                        // When the conversations are small enough, then we shouldn't need to do this
                        // this is just a slight cleanup to remove the extra size check that happens in the
                        // above data load. If it isn't necessary, then we shouldn't do it
                        limitMessagesBasedOnPreviousSize = false
                    }
                } else {
                    cursor = DataSource.getMessages(activity!!, argManager.conversationId)
                }

                messageLoadedCount = cursor.count

                val numbers = argManager.phoneNumbers
                val title = argManager.title

                if (contactMap == null || contactByNameMap == null) {
                    val contacts = DataSource.getContacts(activity!!, numbers)
                    val contactsByName = DataSource.getContactsByNames(activity!!, title)
                    contactMap = fillMapByNumber(numbers, contacts)
                    contactByNameMap = fillMapByName(title, contactsByName)
                }

                val position = findMessagePositionFromId(cursor)

                PerformanceProfiler.logEvent("finished loading messages")

                val firstLoad = adapter == null
                val justUpdatingSendingStatus = !firstLoad && !addedNewMessage

                handler.post {
                    setMessages(cursor, contactMap!!, contactByNameMap!!)
                    draftManager.applyDrafts()

                    if (position != -1) {
                        messageList.scrollToPosition(position)
                    }
                }

                if (Settings.smartReplies && !justUpdatingSendingStatus) {
                    try {
                        val list = mutableListOf<FirebaseTextMessage>()
                        if (cursor.moveToLast()) {
                            do {
                                val message = Message()
                                message.fillFromCursor(cursor)

                                if (MimeType.TEXT_PLAIN == message.mimeType) {
                                    if (message.type == Message.TYPE_RECEIVED) {
                                        list.add(FirebaseTextMessage.createForLocalUser(message.data, message.timestamp))
                                    } else {
                                        list.add(FirebaseTextMessage.createForRemoteUser(message.data, message.timestamp, message.from ?: fragment.argManager.title))
                                    }
                                }
                            } while (cursor.moveToPrevious() && list.size < 10)
                        }

                        val smartReply = FirebaseNaturalLanguage.getInstance().smartReply
                        smartReply.suggestReplies(list.asReversed())
                                .addOnSuccessListener { result ->
                                    handler.post { smartReplyManager.applySuggestions(result.suggestions, firstLoad) }
                                }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }

                PerformanceProfiler.logEvent("finished prepping smart replies")

                if (!argManager.isGroup) {
                    informationUpdater.update()
                }

                if (NotificationConstants.CONVERSATION_ID_OPEN == argManager.conversationId) {
                    Thread.sleep(1000)

                    // this could happen in the background, we don't want to dismiss that then!
                    fragment.notificationManager.dismissNotification()
                    fragment.notificationManager.dismissOnStartup = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun fillMapByName(title: String?, contacts: List<Contact>) = try {
            if (title != null && title.contains(", "))
                ContactUtils.getMessageFromMappingByTitle(title, contacts)
            else HashMap()
        } catch (e: Exception) {
            HashMap<String, Contact>()
        }

    private fun fillMapByNumber(numbers: String, contacts: List<Contact>) = try {
        ContactUtils.getMessageFromMapping(numbers, contacts, DataSource, activity!!)
    } catch (e: Exception) {
        HashMap<String, Contact>()
    }

    private fun findMessagePositionFromId(cursor: Cursor?): Int {
        if (argManager.messageToOpen != -1L && cursor != null && cursor.moveToFirst()) {
            val id = argManager.messageToOpen

            do {
                if (cursor.getLong(0) == id) {
                    return cursor.position
                }
            } while (cursor.moveToNext())
        }

        return -1
    }

    private fun setMessages(messages: Cursor, contactMap: Map<String, Contact>, contactMapByName: Map<String, Contact>) {
        if (adapter != null) {
            adapter?.addMessage(messageList, messages)
        } else {
            adapter = MessageListAdapter(messages, argManager.color,
                    if (Settings.useGlobalThemeColor) Settings.mainColorSet.colorAccent
                    else argManager.colorAccent, argManager.isGroup, fragment)
            adapter?.setFromColorMapper(contactMap, contactMapByName)

            messageList.adapter = adapter
            messageList.animate().withLayer()
                    .alpha(1f).setDuration(100).setStartDelay(0).setListener(null)
        }
    }
    
    companion object {
        const val MESSAGE_LIMIT = 8000
    }
}