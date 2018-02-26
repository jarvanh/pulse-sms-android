package xyz.klinker.messenger.api.implementation.retrofit;

import android.content.Context;
import android.util.Log;

import retrofit2.Call;
import xyz.klinker.messenger.api.implementation.Account;

public class AddMessageRetryableCallback<T> extends LoggingRetryableCallback<T> {

    private final Context context;
    private final long messageId;

    public AddMessageRetryableCallback(Context context, Call<T> call, int totalRetries, long messageId) {
        super(call, totalRetries, "add message");
        this.context = context;
        this.messageId = messageId;
    }

    @Override
    public void onFinalFailure(Call<T> call, Throwable t) {
        super.onFinalFailure(call, t);

        if (context.getApplicationContext() instanceof ApiErrorPersister) {
            ApiErrorPersister persister = (ApiErrorPersister) context.getApplicationContext();
            persister.onAddMessageError(messageId);
        }
    }
}
