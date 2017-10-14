package xyz.klinker.messenger.activity

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.wearable.view.WearableRecyclerView
import android.support.wearable.view.drawer.WearableActionDrawer
import android.support.wearable.view.drawer.WearableDrawerLayout
import android.view.MenuItem
import android.view.View

import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.WearableMessageListAdapter
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.shared_interfaces.IMessageListFragment
import xyz.klinker.messenger.shared.util.DualSimUtils
import xyz.klinker.messenger.shared.util.NotificationUtils
import xyz.klinker.messenger.shared.util.SendUtils
import xyz.klinker.messenger.util.CircularOffsettingHelper
import xyz.klinker.wear.reply.WearableReplyActivity

class MessageListActivity : AppCompatActivity(), IMessageListFragment {

    private val conversation: Conversation by lazy { DataSource.getConversation(this, intent.getLongExtra(CONVERSATION_ID, -1L))!! }

    private val actionDrawer: WearableActionDrawer by lazy { findViewById<View>(R.id.action_drawer) as WearableActionDrawer }
    private val recyclerView: WearableRecyclerView by lazy { findViewById<View>(R.id.recycler_view) as WearableRecyclerView }

    private val manager: LinearLayoutManager by lazy { LinearLayoutManager(this) }
    private val adapter: WearableMessageListAdapter by lazy {
        if (Settings.useGlobalThemeColor) {
            WearableMessageListAdapter(this, manager, null, Settings.mainColorSet.color, Settings.mainColorSet.colorAccent, conversation!!.isGroup)
        } else {
            WearableMessageListAdapter(this, manager, null, conversation!!.colors.color, conversation!!.colors.colorAccent, conversation!!.isGroup)
        }
    }

    private var updatedReceiver: MessageListUpdatedReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_message_list)

        initRecycler()
        loadMessages()
        dismissNotification()

        updatedReceiver = MessageListUpdatedReceiver(this)
        registerReceiver(updatedReceiver,
                MessageListUpdatedReceiver.getIntentFilter())

        actionDrawer.setBackgroundColor(conversation.colors.color)
        actionDrawer.setOnMenuItemClickListener { menuItem ->
            actionDrawer.closeDrawer()

            when (menuItem.itemId) {
                R.id.menu_close -> {
                    finish()
                    true
                }
                R.id.menu_reply -> {
                    WearableReplyActivity.start(this@MessageListActivity)
                    true
                }
                else -> false
            }
        }
    }

    public override fun onDestroy() {
        super.onDestroy()

        try {
            unregisterReceiver(updatedReceiver)
        } catch (e: Exception) {
        }

        adapter.messages.close()
    }

    private fun initRecycler() {
        manager.stackFromEnd = true

        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
        recyclerView.offsettingHelper = CircularOffsettingHelper()
    }

    override fun loadMessages() {
        // doens't really matter here. We are loading everything anyways
        loadMessages(true)
    }

    override fun loadMessages(addedNewMessage: Boolean) {
        Thread {
            val cursor = DataSource.getMessages(this, conversation.id)
            runOnUiThread {
                if (adapter.messages == null) {
                    adapter.messages = cursor
                } else {
                    adapter.addMessage(cursor)
                }
            }
        }.start()
    }

    override fun getConversationId(): Long {
        return conversation.id
    }

    override fun setShouldPullDrafts(pull: Boolean) {

    }

    override fun setDismissOnStartup() {

    }

    override fun setConversationUpdateInfo(text: String) {

    }

    private fun dismissNotification() {
        NotificationManagerCompat.from(this).cancel(conversation.id.toInt())

        ApiUtils.dismissNotification(Account.accountId,
                Account.deviceId, conversation.id)

        NotificationUtils.cancelGroupedNotificationWithNoContent(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = WearableReplyActivity.getResultText(data)
        if (result != null) {
            sendMessage(result)
        }
    }

    private fun sendMessage(text: String) {
        val m = Message()
        m.conversationId = conversationId
        m.type = Message.TYPE_SENDING
        m.data = text
        m.timestamp = System.currentTimeMillis()
        m.mimeType = MimeType.TEXT_PLAIN
        m.read = true
        m.seen = true
        m.from = null
        m.color = null
        m.sentDeviceId = if (Account.exists()) java.lang.Long.parseLong(Account.deviceId) else -1L
        m.simPhoneNumber = if (conversation.simSubscriptionId != null)
            DualSimUtils.get(this).getPhoneNumberFromSimSubscription(conversation.simSubscriptionId!!)
        else
            null

        if (text.isNotEmpty()) {
            DataSource.insertMessage(this, m, m.conversationId)
            loadMessages()

            SendUtils(conversation.simSubscriptionId)
                    .send(this, m.data, conversation.phoneNumbers, null, MimeType.TEXT_PLAIN)
        }
    }

    companion object {

        private val CONVERSATION_ID = "conversation_id"

        fun startActivity(context: Context, conversationId: Long) {
            val intent = Intent(context, MessageListActivity::class.java)
            intent.putExtra(CONVERSATION_ID, conversationId)

            context.startActivity(intent)
        }
    }

}
