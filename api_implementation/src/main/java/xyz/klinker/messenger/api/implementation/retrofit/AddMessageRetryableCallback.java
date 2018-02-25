package xyz.klinker.messenger.api.implementation.retrofit;

import android.util.Log;

import retrofit2.Call;

public class AddMessageRetryableCallback<T> extends LoggingRetryableCallback<T> {

    private final long messageId;

    public AddMessageRetryableCallback(Call<T> call, int totalRetries, long messageId) {
        super(call, totalRetries, "add message");
        this.messageId = messageId;
    }

    @Override
    public void onFinalFailure(Call<T> call, Throwable t) {
        super.onFinalFailure(call, t);
        // TODO: persist this message id so that we can re-upload it when internet is regained
    }
}
