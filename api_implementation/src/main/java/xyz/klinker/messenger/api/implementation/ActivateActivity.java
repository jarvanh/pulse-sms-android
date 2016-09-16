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

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Random;

import xyz.klinker.messenger.api.Api;
import xyz.klinker.messenger.api.entity.LoginResponse;

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

    private String code;
    private int attempts = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.api_activity_activate);

        code = generateActivationCode();

        TextView activationCode = (TextView) findViewById(R.id.activation_code);
        activationCode.setText(code);

        new Thread(new Runnable() {
            @Override
            public void run() {
                queryEndpoint(new ApiUtils().getApi());
            }
        }).start();
    }

    private void queryEndpoint(final Api api) {
        try { Thread.sleep(RETRY_INTERVAL); } catch (Exception e) { }

        Log.v(TAG, "checking activate response");
        final LoginResponse response = api.activate().check(code);

        if (response == null) {
            if (attempts < RETRY_ATTEMPTS) {
                attempts++;
                queryEndpoint(api);
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

    private void activated(LoginResponse response) {
        findViewById(R.id.waiting_to_activate).setVisibility(View.GONE);
        findViewById(R.id.password_confirmation).setVisibility(View.VISIBLE);
        findViewById(R.id.password).requestFocus();
        // TODO save values after asking for password confirmation
    }

    private String generateActivationCode(){
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 8) {
            sb.append(Integer.toHexString(r.nextInt()));
        }

        return sb.toString().substring(0, 8).toUpperCase();
    }

}
