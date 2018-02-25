package xyz.klinker.messenger.api.implementation.retrofit;

import android.util.Log;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import xyz.klinker.messenger.api.implementation.ApiUtils;

public abstract class RetryableCallback<T> implements Callback<T> {

    public abstract void onFinalResponse(Call<T> call, Response<T> response);
    public abstract void onFinalFailure(Call<T> call, Throwable t);

    private int totalRetries = ApiUtils.RETRY_COUNT;
    private static final String TAG = RetryableCallback.class.getSimpleName();
    private final Call<T> call;
    private int retryCount = 0;

    public RetryableCallback(Call<T> call, int totalRetries) {
        this.call = call;
        this.totalRetries = totalRetries;
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        if (!ApiUtils.INSTANCE.isCallSuccessful(response)) {
            if (retryCount++ < totalRetries) {
                Log.v(TAG, "Retrying API Call -  (" + retryCount + " / " + totalRetries + ")");
                try { Log.v(TAG, "Error: " + response.message() + " - " + response.errorBody().string()); } catch (Exception e) { }
                Log.v(TAG, "For call:" + call.request().url().toString());
                retry();
            } else {
                onFinalResponse(call, response);
            }
        } else {
            onFinalResponse(call, response);
        }
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        if (t != null) {
            Log.e(TAG, t.getMessage() != null && !t.getMessage().isEmpty() ? t.getMessage() : "no failure message");
        }

        if (retryCount++ < totalRetries) {
            Log.v(TAG, "On Failure, Retrying API Call: (" + retryCount + " / " + totalRetries + ")");
            Log.v(TAG, "For call: " + call.request().url().toString());
            retry();
        } else {
            onFinalFailure(call, t);
        }
    }

    private void retry() {
        new Thread(() -> {
            try {
                Thread.sleep(4000 * retryCount);
            } catch (InterruptedException e) {

            } finally {
                call.clone().enqueue(RetryableCallback.this);
            }
        }).start();
    }
}