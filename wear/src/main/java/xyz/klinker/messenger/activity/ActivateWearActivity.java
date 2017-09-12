package xyz.klinker.messenger.activity;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;
import java.util.Locale;
import java.util.Random;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.Api;
import xyz.klinker.messenger.api.entity.ContactBody;
import xyz.klinker.messenger.api.entity.ConversationBody;
import xyz.klinker.messenger.api.entity.LoginResponse;
import xyz.klinker.messenger.api.implementation.AccountEncryptionCreator;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.api.implementation.LoginActivity;
import xyz.klinker.messenger.encryption.EncryptionUtils;

public class ActivateWearActivity extends Activity {

    private static final String TAG = "ActivateWearActivity";

    /**
     * Retry the activation request every 5 seconds.
     */
    private static final int RETRY_INTERVAL = 5000;

    /**
     * Number of times to try getting the activation details before giving up.
     * 5 seconds * 60 = 5 minutes.
     */
    private static final int RETRY_ATTEMPTS = 60;

    public static final int RESULT_FAILED = 6666;

    private Api api;
    private String code;
    private int attempts = 0;

    private Handler handler;

    private TextView codeText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_activate);

        handler = new Handler();
        code = generateActivationCode();
        api = ApiUtils.INSTANCE.getApi();

        codeText = (TextView) findViewById(R.id.code);
        codeText.setText(code);

        queryEndpoint();
    }

    private void queryEndpoint() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> new Thread(() -> {
            Log.v(TAG, "checking activate response");
            LoginResponse response;
            try {
                response = api.activate().check(code).execute().body();
            } catch (IOException e) {
                response = null;
            }

            if (response == null) {
                Log.v(TAG, "not activated");
                if (attempts < RETRY_ATTEMPTS) {
                    attempts++;
                    queryEndpoint();
                } else {
                    runOnUiThread(() -> {
                        setResult(RESULT_FAILED);
                        finish();
                    });
                }
            } else {
                Log.v(TAG, "activated");
                final LoginResponse finalResponse = response;
                runOnUiThread(() -> activated(finalResponse));
            }
        }).start(), RETRY_INTERVAL);
    }

    private void activated(final LoginResponse response) {
        findViewById(R.id.waiting_to_activate).setVisibility(View.GONE);
        findViewById(R.id.password_confirmation).setVisibility(View.VISIBLE);
        final EditText password = (EditText) findViewById(R.id.password);

        password.setText(null);
        password.requestFocus();

        findViewById(R.id.confirm).setOnClickListener(v -> {
            Toast.makeText(this, R.string.verifying_password, Toast.LENGTH_SHORT).show();
            checkPassword(response, password.getText().toString());
        });
    }

    private void checkPassword(final LoginResponse response, final String password) {
        if (password == null || password.isEmpty()) {
            Toast.makeText(this, R.string.api_no_password, Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            AccountEncryptionCreator encryptionCreator =
                    new AccountEncryptionCreator(ActivateWearActivity.this, password);
            EncryptionUtils utils = encryptionCreator.createAccountEncryptionFromLogin(response);

            try {
                ConversationBody[] bodies = api.conversation().list(response.accountId).execute().body();
                if (bodies.length > 0) {
                    utils.decrypt(bodies[0].title);
                } else {
                    ContactBody[] contacts = api.contact().list(response.accountId).execute().body();
                    if (contacts.length > 0) {
                        utils.decrypt(contacts[0].name);
                    }
                }

                ApiUtils.INSTANCE.registerDevice(response.accountId,
                        Build.MANUFACTURER + ", " + Build.MODEL, Build.MODEL,
                        false, FirebaseInstanceId.getInstance().getToken());

                setResult(LoginActivity.RESULT_START_NETWORK_SYNC);
                finish();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ActivateWearActivity.this, xyz.klinker.messenger.api.implementation.R.string.api_wrong_password,
                            Toast.LENGTH_LONG).show();
                    activated(response);
                });
            }
        }).start();
    }

    private String generateActivationCode(){
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 8) {
            sb.append(Integer.toHexString(r.nextInt()));
        }

        return sb.toString().substring(0, 8).toUpperCase(Locale.getDefault());
    }

}