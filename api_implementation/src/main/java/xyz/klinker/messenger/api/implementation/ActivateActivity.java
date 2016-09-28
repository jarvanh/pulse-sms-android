/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.api.implementation;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import java.util.Locale;
import java.util.Random;

import xyz.klinker.messenger.api.Api;
import xyz.klinker.messenger.api.entity.ContactBody;
import xyz.klinker.messenger.api.entity.ConversationBody;
import xyz.klinker.messenger.api.entity.LoginResponse;
import xyz.klinker.messenger.encryption.EncryptionUtils;

public class ActivateActivity extends AppCompatActivity {

    private static final String TAG = "ActivateActivity";

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.api_activity_activate);

        code = generateActivationCode();
        api = new ApiUtils().getApi();

        TextView activationCode = (TextView) findViewById(R.id.activation_code);
        activationCode.setText(code);

        new Thread(new Runnable() {
            @Override
            public void run() {
                queryEndpoint();
            }
        }).start();
    }

    private void queryEndpoint() {
        try { Thread.sleep(RETRY_INTERVAL); } catch (Exception e) { }

        Log.v(TAG, "checking activate response");
        final LoginResponse response = api.activate().check(code);

        if (response == null) {
            if (attempts < RETRY_ATTEMPTS) {
                attempts++;
                queryEndpoint();
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setResult(RESULT_FAILED);
                        finish();
                    }
                });
            }
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activated(response);
                }
            });
        }
    }

    private void activated(final LoginResponse response) {
        findViewById(R.id.waiting_to_activate).setVisibility(View.GONE);
        findViewById(R.id.password_confirmation).setVisibility(View.VISIBLE);
        final EditText password = (EditText) findViewById(R.id.password);
        password.setText(null);
        password.requestFocus();

        findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPassword(response, password.getText().toString());
            }
        });
    }

    private void checkPassword(final LoginResponse response, final String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                AccountEncryptionCreator encryptionCreator =
                        new AccountEncryptionCreator(ActivateActivity.this, password);
                EncryptionUtils utils = encryptionCreator.createAccountEncryptionFromLogin(response);

                try {
                    ConversationBody[] bodies = api.conversation().list(response.accountId);
                    if (bodies.length > 0) {
                        utils.decrypt(bodies[0].title);
                    } else {
                        ContactBody[] contacts = api.contact().list(response.accountId);
                        if (contacts.length > 0) {
                            utils.decrypt(contacts[0].name);
                        }
                    }

                    new ApiUtils().registerDevice(response.accountId,
                            Build.MANUFACTURER + ", " + Build.MODEL, Build.MODEL,
                            false, FirebaseInstanceId.getInstance().getToken());

                    setResult(RESULT_OK);
                    finish();
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ActivateActivity.this, R.string.api_wrong_password,
                                    Toast.LENGTH_LONG).show();
                            activated(response);
                        }
                    });
                }
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
