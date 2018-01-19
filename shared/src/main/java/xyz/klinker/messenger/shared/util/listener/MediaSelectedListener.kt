package xyz.klinker.messenger.shared.util.listener

import xyz.klinker.messenger.shared.data.MediaMessage
import xyz.klinker.messenger.shared.data.model.Message

/**
 * Callback for easily notifying the caller when a media has been selected
 */
interface MediaSelectedListener {
    fun onSelected(messageList: List<MediaMessage>, selectedPosition: Int)
    fun onStartDrag(index: Int)
}
