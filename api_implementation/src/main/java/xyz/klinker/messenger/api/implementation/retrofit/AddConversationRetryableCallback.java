package xyz.klinker.messenger.api.implementation.retrofit;

import android.content.Context;

import retrofit2.Call;

public class AddConversationRetryableCallback<T> extends LoggingRetryableCallback<T> {

    private final Context context;
    private final long conversationId;

    public AddConversationRetryableCallback(Context context, Call<T> call, int totalRetries, long conversationId) {
        super(call, totalRetries, "add conversation");
        this.context = context;
        this.conversationId = conversationId;
    }

    @Override
    public void onFinalFailure(Call<T> call, Throwable t) {
        super.onFinalFailure(call, t);

        if (context.getApplicationContext() instanceof ApiErrorPersister) {
            ApiErrorPersister persister = (ApiErrorPersister) context.getApplicationContext();
            persister.onAddConversationError(conversationId);
        }
    }

}
