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

package xyz.klinker.messenger.util;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import xyz.klinker.messenger.R;

/**
 * Helper class for working with permissions and making sure that they are granted.
 */
public class PermissionsUtils {

    private static final int REQUEST_MAIN_PERMISSIONS = 1;

    public static boolean checkRequestMainPermissions(Activity activity) {
        return !checkPermissionGranted(activity, Manifest.permission.READ_CONTACTS) ||
                !checkPermissionGranted(activity, Manifest.permission.READ_SMS) ||
                !checkPermissionGranted(activity, Manifest.permission.READ_PHONE_STATE);
    }

    private static boolean checkPermissionGranted(Activity activity, String permission) {
        return ContextCompat.checkSelfPermission(activity, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void startMainPermissionRequest(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[] {
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.RECEIVE_MMS,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.INTERNET
                }, REQUEST_MAIN_PERMISSIONS);
    }

    public static boolean processPermissionRequest(final Activity activity, int requestCode,
                                                   String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_MAIN_PERMISSIONS) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                new AlertDialog.Builder(activity)
                        .setMessage(R.string.permissions_needed)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startMainPermissionRequest(activity);
                            }
                        })
                        .show();
                return false;
            }
        }

        return false;
    }

    public static boolean isDefaultSmsApp(Context context) {
        try {
            return context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context));
        } catch (NullPointerException e) {
            // thrown by robolectric...
            return true;
        }
    }

    /**
     * Sets the app as the default on the device if it isn't already. If it is, then we don't need
     * to do anything.
     *
     * @param context the current application context.
     */
    public static void setDefaultSmsApp(Context context) {
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.getPackageName());
        context.startActivity(intent);
    }


}
