package xyz.klinker.messenger.activity.notification

import android.content.Context
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.service.jobs.MarkAsSentJob
import xyz.klinker.messenger.shared.util.DualSimUtils
import xyz.klinker.messenger.shared.util.KeyboardLayoutHelper
import xyz.klinker.messenger.shared.util.SendUtils
import xyz.klinker.messenger.shared.util.TimeUtils
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider

class ReplySender(private val activity: MarshmallowReplyActivity, private val dataProvider: ReplyDataProvider, private val animator: ReplyAnimators) {

    private val sendBar: LinearLayout by lazy { activity.findViewById<View>(R.id.send_bar) as LinearLayout }
    private val progressBar: ProgressBar by lazy { activity.findViewById<View>(R.id.send_progress) as ProgressBar }
    private val messageInput: EditText by lazy { activity.findViewById<View>(R.id.message_entry) as EditText }
    private val sendButton: ImageButton by lazy { activity.findViewById<View>(R.id.send_button) as ImageButton }
    private val conversationIndicator: TextView by lazy { activity.findViewById<View>(R.id.conversation_indicator) as TextView }
    private val scrollView: ScrollView by lazy { activity.findViewById<View>(R.id.scroll_view) as ScrollView }

    fun setupViews() {
        if (Settings.useGlobalThemeColor) {
            sendBar.setBackgroundColor(Settings.mainColorSet.color)
            conversationIndicator.setTextColor(Settings.mainColorSet.color)
            conversationIndicator.compoundDrawablesRelative[2] // drawable end
                    .setTintList(ColorStateList.valueOf(Settings.mainColorSet.color))
        } else {
            sendBar.setBackgroundColor(dataProvider.conversation!!.colors.color)
            conversationIndicator.setTextColor(dataProvider.conversation!!.colors.color)
            conversationIndicator.compoundDrawablesRelative[2] // drawable end
                    .setTintList(ColorStateList.valueOf(dataProvider.conversation!!.colors.color))
        }

        conversationIndicator.text = activity.getString(R.string.conversation_with, dataProvider.conversation!!.title)
        conversationIndicator.setOnClickListener({ scrollView.smoothScrollTo(0, 0) })

        sendButton.isEnabled = false
        sendButton.alpha = .5f
        sendButton.setOnClickListener {
            sendButton.isEnabled = false

            hideKeyboard()
            sendMessage()

            animator.alphaOut(sendButton, 200, 0)
            animator.alphaIn(progressBar, 200, 100)

            sendButton.postDelayed({ activity.onBackPressed() }, 1000)
        }

        KeyboardLayoutHelper.applyLayout(messageInput)
        messageInput.hint = activity.getString(R.string.type_message)
        messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (messageInput.text.isNotEmpty()) {
                    sendButton.isEnabled = true
                    sendButton.alpha = 1f
                } else {
                    sendButton.isEnabled = false
                    sendButton.alpha = .5f
                }
            }
        })

        messageInput.setOnEditorActionListener({ _, actionId, keyEvent ->
            if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN &&
                    keyEvent.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_SEND) {
                sendButton.performClick()
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        })
    }

    fun requestFocus() {
        messageInput.postDelayed({
            messageInput.requestFocus()
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(messageInput, InputMethodManager.SHOW_FORCED)
        }, 300)
    }

    fun saveDraft() {
        val text = messageInput.text.toString()
        if (!text.isEmpty() && sendButton.isEnabled) {
            val source = DataSource
            source.insertDraft(activity, dataProvider.conversationId, text, MimeType.TEXT_PLAIN)
        }

        hideKeyboard()
    }

    private fun sendMessage() {
        val message = messageInput.text.toString().trim({ it <= ' ' })

        val m = Message()
        m.conversationId = dataProvider.conversationId
        m.type = Message.TYPE_SENDING
        m.data = message
        m.timestamp = TimeUtils.now
        m.mimeType = MimeType.TEXT_PLAIN
        m.read = true
        m.seen = true
        m.from = null
        m.color = null
        m.sentDeviceId = if (Account.exists()) java.lang.Long.parseLong(Account.deviceId) else -1L
        m.simPhoneNumber = if (dataProvider.conversation?.simSubscriptionId != null)
            DualSimUtils.getPhoneNumberFromSimSubscription(dataProvider.conversation?.simSubscriptionId!!)
        else null

        // we don't have to check zero length, since the button is disabled if zero length
        val messageId = DataSource.insertMessage(activity, m, m.conversationId, true)
        DataSource.readConversation(activity, dataProvider.conversationId)

        Thread {
            SendUtils(dataProvider.conversation?.simSubscriptionId)
                    .send(activity, message, dataProvider.conversation!!.phoneNumbers!!)
            MarkAsSentJob.scheduleNextRun(activity, messageId)
        }.start()

        ConversationListUpdatedReceiver.sendBroadcast(activity, dataProvider.conversationId, activity.getString(R.string.you) + ": " + message, true)
        MessageListUpdatedReceiver.sendBroadcast(activity, dataProvider.conversationId)
        MessengerAppWidgetProvider.refreshWidget(activity)
    }

    fun hideKeyboard() {
        (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(messageInput.windowToken, 0)
    }
}