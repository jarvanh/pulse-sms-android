package xyz.klinker.messenger.shared.util;

import android.content.Context;
import android.database.Cursor;

import java.util.List;

import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;

public class MockableDataSourceWrapper {
    private DataSource source;

    public MockableDataSourceWrapper(DataSource source) {
        this.source = source;
    }

    public List<Message> getMessages(Context context, long conversationId, int numberOfMessages) {
        return source.getMessages(context, conversationId, numberOfMessages);
    }

    public Cursor getUnseenMessages(Context context) {
        return source.getUnseenMessages(context);
    }

    public Conversation getConversation(Context context, long conversationId) {
        return source.getConversation(context, conversationId);
    }
}
