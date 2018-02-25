package xyz.klinker.messenger.api.implementation.retrofit;

import retrofit2.Call;

public class AddConversationRetryableCallback<T> extends LoggingRetryableCallback<T> {

    private final long conversationId;

    public AddConversationRetryableCallback(Call<T> call, int totalRetries, long conversationId) {
        super(call, totalRetries, "add conversation");
        this.conversationId = conversationId;
    }

    @Override
    public void onFinalFailure(Call<T> call, Throwable t) {
        super.onFinalFailure(call, t);
        // TODO: persist this conversation id so that we can re-upload it when internet is regained
    }

}
