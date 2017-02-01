package xyz.klinker.messenger.shared.util.listener;

import java.util.List;

import xyz.klinker.messenger.shared.data.model.Message;

/**
 * Callback for easily notifying the caller when a media has been selected
 */
public interface MediaSelectedListener {
    void onSelected(List<Message> messageList, int selectedPosition);
}
