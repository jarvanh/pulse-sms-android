/*
 * Copyright (C) 2017 Luke Klinker
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

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.fragment.settings.ThemeSettingsFragment;
import xyz.klinker.messenger.shared.activity.AbstractSettingsActivity;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.MmsSettings;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.fragment.settings.FeatureSettingsFragment;
import xyz.klinker.messenger.fragment.settings.GlobalSettingsFragment;
import xyz.klinker.messenger.fragment.settings.MmsConfigurationFragment;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.StringUtils;

public class SettingsActivity extends AbstractSettingsActivity {

    private static final String ARG_SETTINGS_TYPE = "arg_settings_type";
    private static final int TYPE_GLOBAL = 1;
    private static final int TYPE_FEATURE = 2;
    private static final int TYPE_MMS = 3;
    private static final int TYPE_THEME = 4;

    public static void startGlobalSettings(Context context) {
        startWithExtra(context, TYPE_GLOBAL);
    }

    public static void startFeatureSettings(Context context) {
        startWithExtra(context, TYPE_FEATURE);
    }

    public static void startMmsSettings(Context context) {
        startWithExtra(context, TYPE_MMS);
    }

    public static void startThemeSettings(Context context) {
        startWithExtra(context, TYPE_THEME);
    }

    private static void startWithExtra(Context context, int type) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.putExtra(ARG_SETTINGS_TYPE, type);
        context.startActivity(intent);
    }

    private GlobalSettingsFragment globalFragment;
    private FeatureSettingsFragment featureFragment;
    private MmsConfigurationFragment mmsFragment;
    private ThemeSettingsFragment themeFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int type = getIntent().getIntExtra(ARG_SETTINGS_TYPE, TYPE_GLOBAL);
        switch (type) {
            case TYPE_GLOBAL:
                globalFragment = new GlobalSettingsFragment();
                setTitle(StringUtils.titleize(getString(R.string.menu_settings)));
                startFragment(globalFragment);
                break;
            case TYPE_FEATURE:
                featureFragment = new FeatureSettingsFragment();
                setTitle(StringUtils.titleize(getString(R.string.menu_feature_settings)));
                startFragment(featureFragment);
                break;
            case TYPE_MMS:
                mmsFragment = new MmsConfigurationFragment();
                setTitle(R.string.menu_mms_configuration);
                startFragment(mmsFragment);
                break;
            case TYPE_THEME:
                themeFragment = new ThemeSettingsFragment();
                setTitle(getString(R.string.theme_settings_redirect));
                startFragment(themeFragment);
                break;
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // sets it to teal if there is no color selected
        Settings settings = Settings.get(this);
        getToolbar().setBackgroundColor(settings.mainColorSet.getColor());
        ActivityUtils.setStatusBarColor(this, settings.mainColorSet.getColorDark());

        ColorUtils.checkBlackBackground(this);
    }

    private void startFragment(Fragment fragment) {
        getFragmentManager().beginTransaction()
                .replace(R.id.settings_content, fragment)
                .commit();
    }

    @Override
    public void onStart() {
        super.onStart();

        Settings settings = Settings.get(this);
        getToolbar().setBackgroundColor(settings.mainColorSet.getColor());
        ActivityUtils.setStatusBarColor(this, settings.mainColorSet.getColorDark());

        ActivityUtils.setTaskDescription(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (globalFragment != null) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.global_settings, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        Settings.get(this).forceUpdate(this);

        if (mmsFragment == null && themeFragment == null) {
            Intent intent = new Intent(this, MessengerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            MmsSettings.get(this).forceUpdate(this);
        }

        super.onBackPressed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        globalFragment.onActivityResult(requestCode, resultCode, data);
    }

}
