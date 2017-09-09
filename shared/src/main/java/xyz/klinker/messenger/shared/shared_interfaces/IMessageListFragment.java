package xyz.klinker.messenger.shared.shared_interfaces;

public interface IMessageListFragment {

    long getConversationId();

    void setShouldPullDrafts(boolean pull);
    void loadMessages();
    void loadMessages(boolean addedNewMessage);
    void setDismissOnStartup();
    void setConversationUpdateInfo(String text);
}
