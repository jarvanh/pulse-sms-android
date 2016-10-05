package xyz.klinker.messenger.util.listener;

import java.util.List;

import xyz.klinker.messenger.data.model.Message;

/**
 * Callback for easily notifying the caller when a media has been selected
 */
public interface MediaSelectedListener {
    void onSelected(List<Message> messageList, int selectedPosition);
}
