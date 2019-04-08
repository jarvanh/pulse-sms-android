package xyz.klinker.messenger.shared.data.pojo

import java.lang.RuntimeException

enum class NotificationAction {
    REPLY, CALL, READ, DELETE, MUTE, ARCHIVE, SMART_REPLY, EMPTY
}

object NotificationActionMapper {
    fun map(string: String): NotificationAction {
        return when (string) {
            "reply" -> NotificationAction.REPLY
            "call" -> NotificationAction.CALL
            "read" -> NotificationAction.READ
            "delete" -> NotificationAction.DELETE
            "mute" -> NotificationAction.MUTE
            "archive" -> NotificationAction.ARCHIVE
            "smart_reply" -> NotificationAction.SMART_REPLY
            "empty" -> NotificationAction.EMPTY
            else -> throw RuntimeException("no notification action for $string")
        }
    }
}