package xyz.klinker.messenger.shared.data.pojo

import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Message
import java.util.ArrayList

class NotificationConversation {
    var id: Long = 0
    var unseenMessageId: Long = 0
    var title: String? = null
    var snippet: String? = null
    var imageUri: String? = null
    var color: Int = 0
    var ringtoneUri: String? = null
    var ledColor: Int = 0
    var timestamp: Long = 0
    var mute: Boolean = false
    var privateNotification: Boolean = false
    var groupConversation: Boolean = false
    var phoneNumbers: String? = null
    var messages = mutableListOf<NotificationMessage>()
    var realMessages: List<Message> = emptyList()

    fun getFirebaseSmartReplyConversation(): List<FirebaseTextMessage> {
        val list = arrayListOf<FirebaseTextMessage>()

        for (message in realMessages) {
            if (MimeType.TEXT_PLAIN == message.mimeType) {
                if (message.type == Message.TYPE_RECEIVED) {
                    list.add(FirebaseTextMessage.createForLocalUser(message.data, message.timestamp))
                } else {
                    list.add(FirebaseTextMessage.createForRemoteUser(message.data, message.timestamp, message.from ?: title))
                }
            }
        }

        return list
    }
}