package xyz.klinker.messenger.shared.data.pojo;

public class ConversationUpdateInfo {
    public long conversationId;
    public String snippet;
    public boolean read;

    public ConversationUpdateInfo(long conversationId, String snippet, boolean read) {
        this.conversationId = conversationId;
        this.snippet = snippet;
        this.read = read;
    }
}
