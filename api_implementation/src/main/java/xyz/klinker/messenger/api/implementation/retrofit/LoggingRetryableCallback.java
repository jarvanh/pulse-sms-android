package xyz.klinker.messenger.api.implementation.retrofit;

import android.util.Log;

import retrofit2.Call;
import retrofit2.Response;

public class LoggingRetryableCallback<T> extends RetryableCallback<T> {

    private static final String TAG = "RetrofitResponse";

    private String logMessage;

    public LoggingRetryableCallback(Call<T> call, int totalRetries, String logMessage) {
        super(call, totalRetries);
        this.logMessage = logMessage;
    }

    @Override
    public void onFinalResponse(Call<T> call, Response<T> response) {
        Log.v(TAG, logMessage + ": SUCCESS");
    }

    @Override
    public void onFinalFailure(Call<T> call, Throwable t) {
        Log.e(TAG, logMessage + ": FAILED");
    }
}
