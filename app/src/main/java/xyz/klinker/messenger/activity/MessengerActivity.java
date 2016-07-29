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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ContactAdapter;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.fragment.BlacklistFragment;
import xyz.klinker.messenger.fragment.ConversationListFragment;
import xyz.klinker.messenger.fragment.InviteFriendsFragment;
import xyz.klinker.messenger.fragment.ScheduledMessagesFragment;
import xyz.klinker.messenger.fragment.settings.AboutFragment;
import xyz.klinker.messenger.fragment.settings.HelpAndFeedbackFragment;
import xyz.klinker.messenger.fragment.settings.MyAccountFragment;
import xyz.klinker.messenger.fragment.settings.SettingsFragment;
import xyz.klinker.messenger.util.AnimationUtils;
import xyz.klinker.messenger.util.ContactUtils;
import xyz.klinker.messenger.util.ImageUtils;
import xyz.klinker.messenger.util.PermissionsUtils;
import xyz.klinker.messenger.util.listener.BackPressedListener;

/**
 * Main entry point to the app. This will serve for setting up the drawer view, finding
 * conversations and displaying things on the screen to get the user started.
 */
public class MessengerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ConversationListFragment conversationListFragment;
    private FloatingActionButton fab;
    private boolean inSettings = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkInitialStart()) {
            startActivity(new Intent(this, InitialLoadActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_messenger);
        initToolbar();
        initDrawer();
        initFab();

        displayConversations();
    }

    @Override
    public void onStart() {
        super.onStart();
        requestPermissions();
    }

    private void requestPermissions() {
        if (PermissionsUtils.checkRequestMainPermissions(this)) {
            PermissionsUtils.startMainPermissionRequest(this);
        }

        if (!PermissionsUtils.isDefaultSmsApp(this)) {
            PermissionsUtils.setDefaultSmsApp(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        PermissionsUtils.processPermissionRequest(this, requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean checkInitialStart() {
        return Settings.get(this).firstStart;
    }

    private void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (actionBar != null && drawerLayout != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initDrawer() {
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Settings settings = Settings.get(getApplicationContext());
                ((TextView) findViewById(R.id.drawer_header_my_name))
                        .setText(settings.myName);
                ((TextView) findViewById(R.id.drawer_header_my_phone_number))
                        .setText(settings.myPhoneNumber);
                initSnooze();
            }
        }, 250);
    }

    private void initSnooze() {
        ImageButton snooze = (ImageButton) findViewById(R.id.snooze);
        snooze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu menu = new PopupMenu(MessengerActivity.this, view);
                boolean currentlySnoozed = Settings.get(getApplicationContext()).snooze >
                        System.currentTimeMillis();
                menu.inflate(currentlySnoozed ? R.menu.snooze_off : R.menu.snooze);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        long snoozeTil;
                        switch (item.getItemId()) {
                            case R.id.menu_snooze_off:
                                snoozeTil = System.currentTimeMillis();
                                break;
                            case R.id.menu_snooze_1:
                                snoozeTil = System.currentTimeMillis() + (1000 * 60 * 60);
                                break;
                            case R.id.menu_snooze_2:
                                snoozeTil = System.currentTimeMillis() + (1000 * 60 * 60 * 2);
                                break;
                            case R.id.menu_snooze_4:
                                snoozeTil = System.currentTimeMillis() + (1000 * 60 * 60 * 4);
                                break;
                            case R.id.menu_snooze_8:
                                snoozeTil = System.currentTimeMillis() + (1000 * 60 * 60 * 8);
                                break;
                            case R.id.menu_snooze_24:
                                snoozeTil = System.currentTimeMillis() + (1000 * 60 * 60 * 24);
                                break;
                            case R.id.menu_snooze_72:
                                snoozeTil = System.currentTimeMillis() + (1000 * 60 * 60 * 72);
                                break;
                            default:
                                snoozeTil = System.currentTimeMillis();
                                break;
                        }

                        Settings.get(getApplicationContext()).setValue(
                                getString(R.string.pref_snooze), snoozeTil);
                        return true;
                    }
                });

                menu.show();
            }
        });
    }

    private void initFab() {
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), ComposeActivity.class));
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        List<Fragment> fragments = getSupportFragmentManager().getFragments();

        for (Fragment fragment : fragments) {
            if (fragment instanceof BackPressedListener) {
                if (((BackPressedListener) fragment).onBackPressed()) {
                    return;
                }
            }
        }

        if (conversationListFragment == null) {
            Fragment messageListFragment = getSupportFragmentManager().findFragmentById(R.id.message_list_container);
            getSupportFragmentManager().beginTransaction().remove(messageListFragment).commit();
            displayConversations();
            fab.show();
            if (drawerLayout != null) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        } else if (inSettings) {
            clickDefaultDrawerItem();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        closeDrawer();

        if (item.isChecked()) {
            return true;
        }

        if (item.isCheckable()) {
            item.setChecked(true);
        }

        if (drawerLayout != null) {
            if (item.getItemId() == R.id.drawer_conversation) {
                setTitle(R.string.app_title);
            } else if (item.isCheckable()) {
                setTitle(item.getTitle());
            }
        }

        switch(item.getItemId()) {
            case R.id.drawer_conversation:
                return displayConversations();
            case R.id.drawer_schedule:
                return displayScheduledMessages();
            case R.id.drawer_mute_contacts:
                return displayBlacklist();
            case R.id.drawer_invite:
                return displayInviteFriends();
            case R.id.drawer_settings:
                return displaySettings();
            case R.id.drawer_account:
                return displayMyAccount();
            case R.id.drawer_help:
                return displayHelpAndFeedback();
            case R.id.drawer_about:
                return displayAbout();
            case R.id.drawer_call:
                return callContact();
            case R.id.drawer_view_contact:
                return viewContact();
            case R.id.drawer_delete_conversation:
                return deleteConversation();
            case R.id.drawer_conversation_information:
                return conversationInformation();
            case R.id.drawer_contact_settings:
                return contactSettings();
            default:
                return true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!inSettings) {
            getMenuInflater().inflate(R.menu.activity_messenger, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                openDrawer();
                return true;
            case R.id.menu_search:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        getIntent().putExtra(EXTRA_CONVERSATION_ID, conversationListFragment.getExpandedId());

        AnimationUtils.originalRecyclerHeight = -1;
        AnimationUtils.originalFragmentContainerHeight = -1;
    }

    private void clickDefaultDrawerItem() {
        clickNavigationItem(R.id.drawer_conversation);
    }

    private void clickNavigationItem(int itemId) {
        onNavigationItemSelected(navigationView.getMenu().findItem(itemId));
    }

    private void openDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void closeDrawer() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    /*****************************************************************
     *  conversation list drawer options                             *
     *****************************************************************/

    private boolean displayConversations() {
        fab.show();
        invalidateOptionsMenu();
        inSettings = false;

        Bundle extras = getIntent().getExtras();

        if (extras != null && extras.containsKey(EXTRA_CONVERSATION_ID)) {
            Log.v("Messenger Activity", "displaying conversation and messages");
            conversationListFragment = ConversationListFragment
                    .newInstance(getIntent().getLongExtra(EXTRA_CONVERSATION_ID, 0));
            getIntent().getExtras().putLong(EXTRA_CONVERSATION_ID, -1);
        } else {
            Log.v("Messenger Activity", "displaying only conversation");
            conversationListFragment = ConversationListFragment.newInstance();
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.conversation_list_container, conversationListFragment);

        Fragment messageList = getSupportFragmentManager()
                .findFragmentById(R.id.message_list_container);

        if (messageList != null) {
            transaction.remove(messageList);
        }

        transaction.commit();

        return true;
    }

    private boolean displayScheduledMessages() {
        return displayFragmentWithBackStack(new ScheduledMessagesFragment());
    }

    private boolean displayBlacklist() {
        return displayFragmentWithBackStack(new BlacklistFragment());
    }

    private boolean displayInviteFriends() {
        return displayFragmentWithBackStack(new InviteFriendsFragment());
    }

    private boolean displaySettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
    }

    private boolean displayMyAccount() {
        return displayFragmentWithBackStack(new MyAccountFragment());
    }

    private boolean displayHelpAndFeedback() {
        return displayFragmentWithBackStack(new HelpAndFeedbackFragment());
    }

    private boolean displayAbout() {
        return displayFragmentWithBackStack(new AboutFragment());
    }

    private boolean displayFragmentWithBackStack(Fragment fragment) {
        fab.hide();
        invalidateOptionsMenu();
        inSettings = true;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.conversation_list_container, fragment)
                .commit();

        return true;
    }

    /*****************************************************************
     *  message list drawer options                                  *
     *****************************************************************/

    private boolean callContact() {
        if (conversationListFragment.isExpanded()) {
            String uri = "tel:" +
                    conversationListFragment.getExpandedItem().conversation.phoneNumbers;
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse(uri));
            startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    private boolean viewContact() {
        if (conversationListFragment.isExpanded()) {
            Conversation conversation = conversationListFragment.getExpandedItem().conversation;
            String[] names = conversation.title.split(", ");
            String[] numbers = conversation.phoneNumbers.split(", ");
            List<Conversation> conversations = new ArrayList<>();

            for (int i = 0; i < numbers.length; i++) {
                Conversation c = new Conversation();
                c.title = names[i];
                c.phoneNumbers = numbers[i];
                c.imageUri = ContactUtils.findImageUri(numbers[i], this);

                if (ImageUtils.getContactImage(c.imageUri, this) == null) {
                    c.imageUri = null;
                }

                conversations.add(c);
            }

            ContactAdapter adapter = new ContactAdapter(conversations, null);
            RecyclerView recyclerView = new RecyclerView(this);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);

            new AlertDialog.Builder(this)
                    .setView(recyclerView)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();

            return true;
        } else {
            return false;
        }
    }

    private boolean deleteConversation() {
        if (conversationListFragment.isExpanded()) {
            final long conversationId = conversationListFragment.getExpandedId();
            conversationListFragment.onBackPressed();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ConversationListAdapter adapter = conversationListFragment.getAdapter();
                    int position = adapter.findPositionForConversationId(conversationId);
                    if (position != -1) {
                        adapter.deleteItem(position);
                    }
                }
            }, 250);

            return true;
        } else {
            return false;
        }
    }

    private boolean conversationInformation() {
        if (conversationListFragment.isExpanded()) {
            Conversation conversation = conversationListFragment.getExpandedItem().conversation;
            DataSource source = DataSource.getInstance(this);
            source.open();

            new AlertDialog.Builder(this)
                    .setMessage(source.getConversationDetails(conversation))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();

            source.close();
            return true;
        } else {
            return false;
        }
    }

    private boolean contactSettings() {
        if (conversationListFragment.isExpanded()) {
            long conversationId = conversationListFragment.getExpandedId();
            Intent intent = new Intent(this, ContactSettingsActivity.class);
            intent.putExtra(ContactSettingsActivity.EXTRA_CONVERSATION_ID, conversationId);
            startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

}
