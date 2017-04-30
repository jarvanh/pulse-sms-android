package xyz.klinker.messenger.shared.util.listener;

import xyz.klinker.messenger.shared.data.model.Contact;

/**
 * Listener that provides a callback for when the user has selected a contact to share.
 */
public interface AttachContactListener {
    void onContactAttached(String firstName, String lastName, String phone);
}
