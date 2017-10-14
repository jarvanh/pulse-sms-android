package xyz.klinker.messenger.shared.util.listener

import xyz.klinker.messenger.shared.data.model.Message

/**
 * Callback for easily notifying the caller when a media has been selected
 */
interface MediaSelectedListener {
    fun onSelected(messageList: List<Message>, selectedPosition: Int)
}
