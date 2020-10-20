/*
 * Copyright (C) 2020 Luke Klinker
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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import java.util.List;

import xyz.klinker.messenger.api.entity.DeviceBody;
import xyz.klinker.messenger.api.entity.LoginResponse;
import xyz.klinker.messenger.api.entity.SignupResponse;
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper;
import xyz.klinker.messenger.encryption.EncryptionUtils;

/**
 * Activity for logging a user in using the API
 */
public class LoginActivity extends AppCompatActivity {

    public static final String ARG_SKIP_LOGIN = "arg_skip_login";
    public static final String ARG_FORCE_NO_CREATE_ACCOUNT = "arg_no_create_account";
    public static final String ARG_BACKGROUND_COLOR = "arg_background_color";
    public static final String ARG_ACCENT_COLOR = "arg_accent_color";

    public static final int RESULT_START_NETWORK_SYNC = 32;
    public static final int RESULT_START_DEVICE_SYNC = 33;
    public static final int REQUEST_ACTIVATE = 34;

    private boolean isInitial = true;
    private boolean skipLogin = false;

    private FloatingActionButton fab;
    private EditText email;
    private EditText password;
    private EditText passwordConfirmation;
    private EditText name;
    private EditText phoneNumber;
    private TextView errorHint;
    private ProgressDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean touchscreen = getPackageManager().hasSystemFeature("android.hardware.touchscreen");
        if (!touchscreen) {
            startActivityForResult(new Intent(this, ActivateActivity.class), REQUEST_ACTIVATE);
            return;
        }

        setContentView(R.layout.api_activity_login);

        int backgroundColor = getIntent().getIntExtra(ARG_BACKGROUND_COLOR, Integer.MIN_VALUE);
        if (backgroundColor != Integer.MIN_VALUE && backgroundColor != Color.WHITE) {
            findViewById(R.id.initial_layout).setBackgroundColor(backgroundColor);
            findViewById(R.id.login_dialog).setBackgroundColor(backgroundColor);
            findViewById(R.id.signup_dialog).setBackgroundColor(backgroundColor);

            int accentColor = getIntent().getIntExtra(ARG_ACCENT_COLOR, Integer.MIN_VALUE);
            if (accentColor != Integer.MIN_VALUE) {
                FloatingActionButton signupFab = (FloatingActionButton) findViewById(R.id.signup_fab);
                FloatingActionButton loginFab = (FloatingActionButton) findViewById(R.id.login_fab);

                signupFab.setBackgroundTintList(ColorStateList.valueOf(accentColor));
                loginFab.setBackgroundTintList(ColorStateList.valueOf(accentColor));

                TextView forgotPassword = (TextView) findViewById(R.id.forgot_password);
                forgotPassword.setTextColor(accentColor);
            }
        }

        skipLogin = getIntent().getBooleanExtra(ARG_SKIP_LOGIN, false);
        if (!skipLogin || !hasTelephony(this)) {
            // we should only skip the login if they are on a phone. A tablet should never be able to login
            setUpInitialLayout();
            new Handler().postDelayed(() -> circularRevealIn(), 100);
        } else {
            onBackPressed();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ACTIVATE && resultCode == RESULT_OK) {
            setResult(RESULT_START_NETWORK_SYNC);
            finish();
        } else if (requestCode == REQUEST_ACTIVATE && resultCode == ActivateActivity.RESULT_FAILED) {
            setResult(ActivateActivity.RESULT_FAILED);
            finish();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void setUpInitialLayout() {
        Button login = (Button) findViewById(R.id.login);
        Button signup = (Button) findViewById(R.id.signup);
        View signupFailed = findViewById(R.id.signup_failed);
        Button skip = (Button) findViewById(R.id.skip);

        String phoneNumber = getPhoneNumber();
        if (!hasTelephony(this) && (phoneNumber == null || phoneNumber.isEmpty())) {
            signup.setEnabled(false);
            signupFailed.setVisibility(View.VISIBLE);
            findViewById(R.id.skip_holder).setVisibility(View.GONE);
        } else if (getIntent().getBooleanExtra(ARG_FORCE_NO_CREATE_ACCOUNT, false)) {
            signup.setEnabled(false);
            findViewById(R.id.skip_holder).setVisibility(View.GONE);
        }

        login.setOnClickListener(view -> login());
        signup.setOnClickListener(view -> signup());
        skip.setOnClickListener(view -> onBackPressed());
    }

    public static boolean hasTelephony(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    protected void login() {
        slideLoginIn();
        isSignUp = false;

        fab = (FloatingActionButton) findViewById(R.id.login_fab);
        email = (EditText) findViewById(R.id.login_email);
        password = (EditText) findViewById(R.id.login_password);
        View forgotPassword = findViewById(R.id.forgot_password);

        fab.setOnClickListener(view -> performLogin());

        password.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            boolean handled = false;

            if ((keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                    keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) ||
                    actionId == EditorInfo.IME_ACTION_DONE) {
                fab.performClick();
                handled = true;
            }

            return handled;
        });

        forgotPassword.setOnClickListener(view -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://messenger.klinkerapps.com/forgot_password.html"));

            try {
                startActivity(browserIntent);
            } catch(Exception e) {
                // no browser found
                Toast.makeText(view.getContext(), "No browser app found.", Toast.LENGTH_SHORT).show();
            }
        });

        fab.hide();
        attachLoginTextWatcher(email);
        attachLoginTextWatcher(password);
    }

    protected void signup() {
        slideSignUpIn();
        isSignUp = true;

        fab = (FloatingActionButton) findViewById(R.id.signup_fab);
        email = (EditText) findViewById(R.id.signup_email);
        password = (EditText) findViewById(R.id.signup_password);
        passwordConfirmation = (EditText) findViewById(R.id.signup_password_confirmation);
        name = (EditText) findViewById(R.id.signup_name);
        phoneNumber = (EditText) findViewById(R.id.signup_phone_number);
        errorHint = (TextView) findViewById(R.id.signup_error_hint);

        fab.setOnClickListener(view -> performSignup());

        fab.hide();
        attachSignupTextWatcher(email);
        attachSignupTextWatcher(password);
        attachSignupTextWatcher(passwordConfirmation);
        attachSignupTextWatcher(name);
        attachSignupTextWatcher(phoneNumber);

        email.setText(getEmail());
        name.setText(getName());

        String number = getPhoneNumber();
        phoneNumber.setText(number);
        phoneNumber.setEnabled(true);
    }

    @SuppressLint("ApplySharedPref")
    private void performLogin() {
        dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.api_connecting));
        dialog.show();

        new Thread(() -> {
            ApiUtils utils = ApiUtils.INSTANCE;
            final LoginResponse response = utils.login(email.getText().toString(),
                    password.getText().toString());

            if (response == null) {
                runOnUiThread(() -> {
                    try {
                        dialog.dismiss();
                    } catch (Exception e) { }
                    setResult(RESULT_CANCELED);
                    Toast.makeText(getApplicationContext(), R.string.api_login_error,
                            Toast.LENGTH_SHORT).show();
                });
            } else {
                AccountEncryptionCreator encryptionCreator =
                        new AccountEncryptionCreator(LoginActivity.this, password.getText().toString());
                EncryptionUtils encryptionUtils = encryptionCreator.createAccountEncryptionFromLogin(response);

                if (response.passcode != null && !response.passcode.isEmpty() && !response.passcode.equals("null")) {
                    SharedPreferences sharedPrefs = encryptionCreator.getSharedPrefs(LoginActivity.this);
                    sharedPrefs.edit().putString("private_conversations_passcode", encryptionUtils.decrypt(response.passcode)).commit();
                }

                // whenever they sign in, it should assign either the trial or the subscriber status to the phone.
                // the account encryption creator automatically creates the FREE_TRIAL status if they are not LIFETIME
                SharedPreferences sharedPrefs = encryptionCreator.getSharedPrefs(LoginActivity.this);
                sharedPrefs.edit().putInt(getString(R.string.api_pref_subscription_type), Account.INSTANCE.getSubscriptionType() == Account.SubscriptionType.LIFETIME ?
                        Account.SubscriptionType.LIFETIME.getTypeCode() : Account.SubscriptionType.SUBSCRIBER.getTypeCode()).commit();

                addDevice(utils, response.accountId, hasTelephony(LoginActivity.this), false);
                AnalyticsHelper.accountLoggedIn(LoginActivity.this);
            }
        }).start();
    }

    private void performSignup() {
        if (password.getText().length() > 55) {
            Toast.makeText(getApplicationContext(), R.string.api_password_to_long,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.api_connecting));
        dialog.show();

        new Thread(() -> {
            ApiUtils utils = ApiUtils.INSTANCE;
            final SignupResponse response = utils.signup(email.getText().toString(),
                    password.getText().toString(), name.getText().toString(),
                    phoneNumber.getText().toString());

            if (response == null) {
                runOnUiThread(() -> {
                    try {
                        dialog.dismiss();
                    } catch (Exception e) { }
                    setResult(RESULT_CANCELED);
                    Toast.makeText(getApplicationContext(), R.string.api_email_taken,
                            Toast.LENGTH_SHORT).show();
                });
            } else {
                // This will automatically give them the free trial status, unless they have the LIFETIME
                AccountEncryptionCreator encryptionCreator =
                        new AccountEncryptionCreator(LoginActivity.this, password.getText().toString());
                encryptionCreator.createAccountEncryptionFromSignup(
                        name.getText().toString(), phoneNumber.getText().toString(),
                        response);

                addDevice(utils, response.accountId, true, true);
                AnalyticsHelper.accountSignedUp(LoginActivity.this);

                applyAccountSettings(encryptionCreator);
            }
        }).start();
    }

    private Integer addDevice(final ApiUtils utils, final String accountId, final boolean primary,
                           final boolean deviceSync) {
        Integer deviceId = utils.registerDevice(accountId,
                Build.MANUFACTURER + ", " + Build.MODEL, Build.MODEL,
                primary, getFirebaseId());

        if (deviceId != null) {
            Account account = Account.INSTANCE;
            account.setDeviceId(this, Long.toString(deviceId));
            account.setPrimary(this, primary);

            runOnUiThread(() -> {
                try {
                    dialog.dismiss();
                } catch (Exception e) { }
                setResult(deviceSync ? RESULT_START_DEVICE_SYNC : RESULT_START_NETWORK_SYNC);
                close();
            });
        } else {
            DeviceBody[] devices = utils.getDevices(accountId);
            if (devices == null) {
                failAddDevice(utils, accountId);
            } else {
                int primaryLocation = -1;
                for (int i = 0; i < devices.length; i++) {
                    if (devices[i].primary) {
                        primaryLocation = i;
                        break;
                    }
                }

                if (primaryLocation == -1) {
                    failAddDevice(utils, accountId);
                    return null;
                }

                final DeviceBody device = devices[primaryLocation];

                if (device != null && device.name != null && device.name.equals(Build.MODEL)) {
                    utils.removeDevice(accountId, device.id);

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) { }

                    addDevice(utils, accountId, true, false);
                } else {
                    runOnUiThread(() -> {
                        String message = getString(R.string.api_add_second_primary_device,
                                device.name);

                        new AlertDialog.Builder(LoginActivity.this)
                                .setMessage(message)
                                .setPositiveButton(R.string.api_yes, (dialogInterface, i) -> new Thread(() -> {
                                    utils.removeDevice(accountId, device.id);
                                    Integer deviceId1 = addDevice(utils, accountId, true, false);

                                    if (deviceId1 != null) {
                                        utils.updatePrimaryDevice(accountId, deviceId1.toString());
                                    }
                                }).start())
                                .setNegativeButton(R.string.api_no, (dialogInterface, i) -> new Thread(() ->
                                        addDevice(utils, accountId, false, false)).start())
                                .show();
                    });
                }
            }
        }

        return deviceId;
    }

    private boolean isSignUp = true;
    private void failAddDevice(ApiUtils apiUtils, String accountId) {
        Log.v("LoginActivity", "failed and closing");
        if (isSignUp) {
            apiUtils.deleteAccount(accountId);
        }

        Account account = Account.INSTANCE;
        account.setDeviceId(this, null);
        account.setPrimary(this, false);

        runOnUiThread(() -> {
            if (isPossiblyKindleFire()) {
                new AlertDialog.Builder(LoginActivity.this)
                        .setMessage(R.string.api_amazon_fire)
                        .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {})
                        .show();
            } else if (!isGooglePlayServicesAvailable(this)) {
                Toast.makeText(getApplicationContext(), R.string.api_device_error_gps,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.api_device_error,
                        Toast.LENGTH_SHORT).show();
            }

            recreate();
        });
    }

    private boolean isPossiblyKindleFire() {
        return android.os.Build.MANUFACTURER.toLowerCase().equals("amazon")
                || android.os.Build.MODEL.toLowerCase().contains("kindle")
                || android.os.Build.MODEL.toLowerCase().contains("fire")
                || android.os.Build.MODEL.startsWith("KF");
    }

    private boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 2404).show();
            }

            return false;
        }
        
        return true;
    }

    private void applyAccountSettings(AccountEncryptionCreator encryptionCreator) {
        SharedPreferences sharedPrefs = encryptionCreator.getSharedPrefs(LoginActivity.this);
        String accountId = Account.INSTANCE.getAccountId();
        ApiUtils api = ApiUtils.INSTANCE;

        String passcode = sharedPrefs.getString("private_conversations_passcode", null);
        if (passcode != null && !passcode.isEmpty()) {
            api.updatePrivateConversationsPasscode(accountId, passcode);
        }

        api.updateBaseTheme(accountId, sharedPrefs.getString("base_theme", "day_night"));
        api.updateApplyToolbarColor(accountId, sharedPrefs.getBoolean("apply_primary_color_toolbar", true));
        api.updateUseGlobalTheme(accountId, sharedPrefs.getBoolean("apply_theme_globally", false));
        api.updateMessageTimestamp(accountId, sharedPrefs.getBoolean("message_timestamp", false));
        api.updateConversationCategories(accountId, sharedPrefs.getBoolean("conversation_categories", true));
        api.updatePrimaryThemeColor(accountId, sharedPrefs.getInt("global_primary_color", Color.parseColor("#1775D2")));
        api.updatePrimaryDarkThemeColor(accountId, sharedPrefs.getInt("global_primary_dark_color", Color.parseColor("#1665C0")));
        api.updateAccentThemeColor(accountId, sharedPrefs.getInt("global_accent_color", Color.parseColor("#FF6E40")));
    }

    private void attachLoginTextWatcher(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isFilled(email) && isFilled(password) && isValidEmail(email.getText())) {
                    fab.show();
                } else {
                    fab.hide();
                }
            }
        });
    }

    private void attachSignupTextWatcher(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isFilled(email) && isFilled(password) && isFilled(passwordConfirmation) &&
                        isFilled(name) && isFilled(phoneNumber) && isValidEmail(email.getText()) &&
                        passwordConfirmation.getText().toString().equals(password.getText().toString())) {
                    fab.show();
                    errorHint.setVisibility(View.GONE);
                } else {
                    fab.hide();
                    errorHint.setVisibility(View.VISIBLE);

                    if (!isFilled(email)) {
                        errorHint.setText(R.string.api_no_email);
                    } else if (!isValidEmail(email.getText())) {
                        errorHint.setText(R.string.api_email_invalid);
                    } else if (!isFilled(password)) {
                        errorHint.setText(R.string.api_no_password);
                    } else if (!isFilled(passwordConfirmation)) {
                        errorHint.setText(R.string.api_no_password_confirmation);
                    } else if (!passwordConfirmation.getText().toString()
                            .equals(password.getText().toString())) {
                        errorHint.setText(R.string.api_password_mismatch);
                    } else if (!isFilled(name)) {
                        errorHint.setText(R.string.api_no_name);
                    } else if (!isFilled(phoneNumber)) {
                        errorHint.setText(R.string.api_no_phone_number);
                    }
                }
            }
        });
    }

    private boolean isFilled(EditText editText) {
        return editText.getText() != null && editText.getText().length() != 0;
    }

    private void circularRevealIn() {
        View view = findViewById(R.id.initial_layout);
        view.setVisibility(View.VISIBLE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int cx = view.getWidth() / 2;
                int cy = view.getHeight() / 2;
                float finalRadius = (float) Math.hypot(cx, cy);
                ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius).start();
            } else {
                view.setAlpha(0f);
                view.animate().alpha(1f).start();
            }
        } catch (Exception e) {
            finish();
        }
    }

    private void circularRevealOut() {
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
                    view.setVisibility(View.INVISIBLE);
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

    private void slideLoginIn() {
        slideIn(findViewById(R.id.login_layout));
    }

    private void slideSignUpIn() {
        slideIn(findViewById(R.id.signup_layout));
    }

    private void slideIn(View view) {
        isInitial = false;
        final View initial = findViewById(R.id.initial_layout);

        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationX(view.getWidth());
        view.animate()
                .alpha(1f)
                .translationX(0)
                .setListener(null)
                .start();

        initial.animate()
                .alpha(0f)
                .translationX(-1 * initial.getWidth())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        initial.setVisibility(View.INVISIBLE);
                        initial.setTranslationX(0);
                    }
                }).start();
    }

    private void slideOut() {
        isInitial = true;
        final View visible = findVisibleHolder();
        View initial = findViewById(R.id.initial_layout);

        visible.animate()
                .alpha(0f)
                .translationX(visible.getWidth())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        visible.setVisibility(View.INVISIBLE);
                        visible.setTranslationX(0);
                    }
                }).start();

        initial.setVisibility(View.VISIBLE);
        initial.setAlpha(0f);
        initial.setTranslationX(-1 * initial.getWidth());
        initial.animate()
                .alpha(1f)
                .translationX(0)
                .setListener(null)
                .start();
    }

    private View findVisibleHolder() {
        View initial = findViewById(R.id.initial_layout);
        View login = findViewById(R.id.login_layout);
        View signup = findViewById(R.id.signup_layout);

        if (initial.getVisibility() != View.INVISIBLE) {
            return initial;
        } else if (login.getVisibility() != View.INVISIBLE) {
            return login;
        } else {
            return signup;
        }
    }

    @Override
    public void onBackPressed() {
        if (skipLogin) {
            close();
        } else if (isInitial) {
            circularRevealOut();
        } else {
            slideOut();
        }
    }

    protected void close() {
        finish();
        overridePendingTransition(0, 0);
    }

    private String getEmail() {
//        try {
//            Cursor cursor = getContentResolver()
//                    .query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
//
//            if (cursor != null && cursor.moveToFirst()) {
//                cursor.moveToFirst();
//                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
//                cursor.close();
//                return name;
//            } else {
//                try {
//                    cursor.close();
//                } catch (Exception e) { }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        return null;
    }

    private String getName() {
        try {
            Cursor cursor = getContentResolver()
                    .query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                cursor.moveToFirst();
                String name = cursor.getString(cursor.getColumnIndex("display_name"));
                cursor.close();
                return name;
            } else {
                try {
                    cursor.close();
                } catch (Exception e) { }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getPhoneNumber() {
        String number = getLollipopPhoneNumber();

        if (number == null || number.isEmpty()) {
            TelephonyManager telephonyManager = (TelephonyManager)
                    getSystemService(Context.TELEPHONY_SERVICE);
            try {
                number = telephonyManager.getLine1Number();
            } catch (SecurityException e) {
                number = "";
            }
        }

        return PhoneNumberUtils.stripSeparators(number);
    }

    private String getLollipopPhoneNumber() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionManager manager = SubscriptionManager.from(this);
                List<SubscriptionInfo> availableSims = manager.getActiveSubscriptionInfoList();

                if (availableSims != null && availableSims.size() > 0) {
                    return availableSims.get(0).getNumber();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getFirebaseId() {
        return FirebaseInstanceId.getInstance().getToken();
    }

    private boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}
