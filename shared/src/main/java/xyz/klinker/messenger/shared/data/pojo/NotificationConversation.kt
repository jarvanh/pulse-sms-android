package xyz.klinker.messenger.shared.data.pojo

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
}