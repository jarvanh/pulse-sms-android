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

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.fragment.settings.SettingsFragment;
import xyz.klinker.messenger.util.ColorUtils;

public class SettingsActivity extends AppCompatActivity {

    private SettingsFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fragment = new SettingsFragment();

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // sets it to teal if there is no color selected
        Settings settings = Settings.get(this);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(settings.globalColorSet.color));
        getWindow().setStatusBarColor(settings.globalColorSet.colorDark);

        ColorUtils.updateRecentsEntry(this);
        ColorUtils.checkBlackBackground(this);
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
        Settings.get(this).forceUpdate();

        Intent intent = new Intent(this, MessengerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        super.onBackPressed();
    }

}
