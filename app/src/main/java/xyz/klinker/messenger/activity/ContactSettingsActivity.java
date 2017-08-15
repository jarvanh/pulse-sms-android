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
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import xyz.klinker.messenger.fragment.settings.ContactSettingsFragment;
import xyz.klinker.messenger.shared.MessengerActivityExtras;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.util.ColorUtils;

/**
 * Activity for changing contact settings_global.
 */
public class ContactSettingsActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";

    private ContactSettingsFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fragment = ContactSettingsFragment.newInstance(
                getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1));

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ColorUtils.checkBlackBackground(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        ActivityUtils.setTaskDescription(this, fragment.conversation.title, fragment.conversation.colors.color);
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
        fragment.saveSettings();

        Intent intent = new Intent(this, MessengerActivity.class);
        intent.putExtra(MessengerActivityExtras.INSTANCE.getEXTRA_CONVERSATION_ID(),
                getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        super.onBackPressed();
    }

}
