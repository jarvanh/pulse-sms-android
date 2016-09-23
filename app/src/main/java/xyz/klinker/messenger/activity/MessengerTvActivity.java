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

package xyz.klinker.messenger.activity;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.fragment.TvBrowseFragment;
import xyz.klinker.messenger.util.TimeUtils;

/**
 * Activity for displaying content on your tv.
 */
public class MessengerTvActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings settings = Settings.get(this);
        if (settings.accountId == null) {
            startActivity(new Intent(this, InitialLoadTvActivity.class));
            finish();
            return;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new TvBrowseFragment())
                .commit();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SYSTEM_ALERT_WINDOW) !=
                PackageManager.PERMISSION_GRANTED &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent permission =
                        new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                permission.setData(Uri.parse("package:" + getPackageName()));
                startActivity(permission);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        
        boolean isDarkTheme = Settings.get(this).darkTheme;
        if (!isDarkTheme) {
            boolean isNight = TimeUtils.isNight() && !Settings.get(this).onlyLightTheme;
            getDelegate().setLocalNightMode(isNight ?
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

}
