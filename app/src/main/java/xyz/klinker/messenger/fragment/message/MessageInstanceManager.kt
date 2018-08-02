package xyz.klinker.messenger.fragment.message

import android.app.Notification
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.util.AndroidVersionUtil

class MessageInstanceManager(private val fragment: MessageListFragment) {

    private val arguments
        get() = fragment.arguments

    val conversationId: Long by lazy { arguments!!.getLong(ARG_CONVERSATION_ID) }
    val title: String by lazy { arguments!!.getString(ARG_TITLE) }
    val phoneNumbers: String by lazy { arguments!!.getString(ARG_PHONE_NUMBERS) }
    val imageUri: String? by lazy { arguments!!.getString(ARG_IMAGE_URI) }
    val color: Int by lazy { arguments!!.getInt(ARG_COLOR) }
    val colorDark: Int by lazy { arguments!!.getInt(ARG_COLOR_DARKER) }
    val colorAccent: Int by lazy { arguments!!.getInt(ARG_COLOR_ACCENT) }
    val isMuted: Boolean by lazy { arguments!!.getBoolean(ARG_MUTE_CONVERSATION) }
    val isRead: Boolean by lazy { arguments!!.getBoolean(ARG_READ) }
    val isGroup: Boolean by lazy { arguments!!.getBoolean(ARG_IS_GROUP) }
    val isArchived: Boolean by lazy { arguments!!.getBoolean(ARG_IS_ARCHIVED) }

    val messageToOpen: Long by lazy { arguments!!.getLong(ARG_MESSAGE_TO_OPEN_ID, -1L) }
    val limitMessages: Boolean by lazy { arguments!!.getBoolean(ARG_LIMIT_MESSAGES) }
    val shouldOpenKeyboard: Boolean by lazy { fragment.activity?.intent?.getBooleanExtra(MessengerActivityExtras.EXTRA_SHOULD_OPEN_KEYBOARD, false) ?: false }
    val notificationInputDraft: String?
        get() {
            return if (AndroidVersionUtil.isAndroidP) {
                fragment.activity?.intent?.getStringExtra(Notification.EXTRA_REMOTE_INPUT_DRAFT)
            } else null
        }

    companion object {
        val ARG_TITLE = "title"
        val ARG_PHONE_NUMBERS = "phone_numbers"
        val ARG_COLOR = "color"
        val ARG_COLOR_DARKER = "color_darker"
        val ARG_COLOR_ACCENT = "color_accent"
        val ARG_IS_GROUP = "is_group"
        val ARG_CONVERSATION_ID = "conversation_id"
        val ARG_MUTE_CONVERSATION = "mute_conversation"
        val ARG_MESSAGE_TO_OPEN_ID = "message_to_open"
        val ARG_READ = "read"
        val ARG_IMAGE_URI = "image_uri"
        val ARG_IS_ARCHIVED = "is_archived"
        val ARG_LIMIT_MESSAGES = "limit_messages"

        fun newInstance(conversation: Conversation, messageToOpenId: Long = -1, limitMessages: Boolean = true): MessageListFragment {
            val fragment = MessageListFragment()

            val args = Bundle()
            args.putString(ARG_TITLE, conversation.title)
            args.putString(ARG_PHONE_NUMBERS, conversation.phoneNumbers)
            args.putInt(ARG_COLOR, conversation.colors.color)
            args.putInt(ARG_COLOR_DARKER, conversation.colors.colorDark)
            args.putInt(ARG_COLOR_ACCENT, conversation.colors.colorAccent)
            args.putBoolean(ARG_IS_GROUP, conversation.isGroup)
            args.putLong(ARG_CONVERSATION_ID, conversation.id)
            args.putBoolean(ARG_MUTE_CONVERSATION, conversation.mute)
            args.putBoolean(ARG_READ, conversation.read)
            args.putString(ARG_IMAGE_URI, conversation.imageUri)
            args.putBoolean(ARG_IS_ARCHIVED, conversation.archive)
            args.putBoolean(ARG_LIMIT_MESSAGES, limitMessages)

            if (messageToOpenId != -1L) {
                args.putLong(ARG_MESSAGE_TO_OPEN_ID, messageToOpenId)
            }

            fragment.arguments = args
            return fragment
        }
    }
}