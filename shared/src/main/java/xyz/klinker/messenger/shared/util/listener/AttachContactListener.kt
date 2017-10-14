package xyz.klinker.messenger.shared.util.listener

/**
 * Listener that provides a callback for when the user has selected a contact to share.
 */
interface AttachContactListener {
    fun onContactAttached(firstName: String, lastName: String, phone: String)
}
