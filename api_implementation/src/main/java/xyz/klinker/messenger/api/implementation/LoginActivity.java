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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Button;

/**
 * Activity for logging a user in using the API
 */
public class LoginActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.api_activity_login);
        setUpInitialLayout();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                animateLayoutIn();
            }
        }, 100);
    }

    private void setUpInitialLayout() {
        Button login = (Button) findViewById(R.id.login);
        Button signup = (Button) findViewById(R.id.signup);
        View signupFailed = findViewById(R.id.signup_failed);
        Button skip = (Button) findViewById(R.id.skip);

        if (getPhoneNumber() == null) {
            signup.setEnabled(false);
            signupFailed.setVisibility(View.VISIBLE);
        }

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void animateLayoutIn() {
        View view = findViewById(R.id.initial_layout);
        view.setVisibility(View.VISIBLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int cx = view.getWidth() / 2;
            int cy = view.getHeight() / 2;
            float finalRadius = (float) Math.hypot(cx, cy);
            ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius).start();
        } else {
            view.setAlpha(0f);
            view.animate().alpha(1f).start();
        }
    }

    private void animateLayoutOut() {
        final View view = findVisibleHolder();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int cx = view.getWidth() / 2;
            int cy = view.getHeight() / 2;
            float initialRadius = (float) Math.hypot(cx, cy);
            Animator anim =
                    ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setVisibility(View.GONE);
                    close();
                }
            });

            anim.start();
        } else {
            view.animate().alpha(0f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    close();
                }
            }).start();
        }
    }

    private View findVisibleHolder() {
        View initial = findViewById(R.id.initial_layout);
        View login = findViewById(R.id.login_layout);
        View signup = findViewById(R.id.signup_layout);

        if (initial.getVisibility() != View.GONE) {
            return initial;
        } else if (login.getVisibility() != View.GONE) {
            return login;
        } else {
            return signup;
        }
    }

    @Override
    public void onBackPressed() {
        animateLayoutOut();
    }

    private void close() {
        finish();
        overridePendingTransition(0, 0);
    }

    private String getName() {
        Cursor cursor = getContentResolver()
                .query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            cursor.moveToFirst();
            String name = cursor.getString(cursor.getColumnIndex("display_name"));
            cursor.close();
            return name;
        }

        return null;
    }

    private String getPhoneNumber() {
        TelephonyManager telephonyManager = (TelephonyManager)
                getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getLine1Number();
    }

}
