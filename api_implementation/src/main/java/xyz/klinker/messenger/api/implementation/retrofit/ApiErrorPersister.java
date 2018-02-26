package xyz.klinker.messenger.api.implementation.retrofit;

public interface ApiErrorPersister {

    void onAddConversationError(long conversationId);
    void onAddMessageError(long messageId);

}
