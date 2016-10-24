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
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ContactAdapter;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.api.implementation.LoginActivity;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.fragment.ArchivedConversationListFragment;
import xyz.klinker.messenger.fragment.BlacklistFragment;
import xyz.klinker.messenger.fragment.ConversationListFragment;
import xyz.klinker.messenger.fragment.InviteFriendsFragment;
import xyz.klinker.messenger.fragment.MessageListFragment;
import xyz.klinker.messenger.fragment.ScheduledMessagesFragment;
import xyz.klinker.messenger.fragment.SearchFragment;
import xyz.klinker.messenger.fragment.settings.AboutFragment;
import xyz.klinker.messenger.fragment.settings.HelpAndFeedbackFragment;
import xyz.klinker.messenger.fragment.settings.MyAccountFragment;
import xyz.klinker.messenger.service.ApiDownloadService;
import xyz.klinker.messenger.service.NotificationService;
import xyz.klinker.messenger.service.SubscriptionExpirationCheckService;
import xyz.klinker.messenger.util.ColorUtils;
import xyz.klinker.messenger.util.ContactUtils;
import xyz.klinker.messenger.util.ImageUtils;
import xyz.klinker.messenger.util.PermissionsUtils;
import xyz.klinker.messenger.util.PhoneNumberUtils;
import xyz.klinker.messenger.util.StringUtils;
import xyz.klinker.messenger.util.TimeUtils;
import xyz.klinker.messenger.util.UpdateUtils;
import xyz.klinker.messenger.util.listener.BackPressedListener;
import xyz.klinker.messenger.util.listener.ContactClickedListener;
import xyz.klinker.messenger.widget.MessengerAppWidgetProvider;

/**
 * Main entry point to the app. This will serve for setting up the drawer view, finding
 * conversations and displaying things on the screen to get the user started.
 */
public class MessengerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        MaterialSearchView.OnQueryTextListener, MaterialSearchView.SearchViewListener {

    public static boolean START_DOWNLOAD_SERVICE = false;

    public static final String EXTRA_START_MY_ACCOUNT = "start_my_account";
    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_FROM_NOTIFICATION = "from_notification";
    public static final String EXTRA_MESSAGE_ID = "message_id";
    public static final String EXTRA_CONVERSATION_NAME = "conversation_name";

    public static final int REQUEST_ONBOARDING = 101;
    public static final int REQUEST_CALL_PERMISSION = 104;

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ConversationListFragment conversationListFragment;
    private Fragment otherFragment;
    private SearchFragment searchFragment;
    private FloatingActionButton fab;
    private MaterialSearchView searchView;
    private boolean inSettings = false;

    private BroadcastReceiver downloadReceiver;

    private boolean loaded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_messenger);
        initToolbar();
        initDrawer();
        initFab();
        configureGlobalColors();

        dismissIfFromNotification();

        if (checkInitialStart()) {
            boolean hasTelephone = getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

            if (!hasTelephone) {
                // if it isn't a phone, then we want to go straight to the login
                // and skip the onboarding, since they would have done it on their phone
                Intent login = new Intent(this, InitialLoadActivity.class);
                startActivity(login);
                finish();
            } else {
                startActivityForResult(
                        new Intent(this, OnboardingActivity.class),
                        REQUEST_ONBOARDING
                );
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        requestPermissions();

        if (conversationListFragment != null && conversationListFragment.isExpanded()) {
            NotificationService.CONVERSATION_ID_OPEN = conversationListFragment.getExpandedId();
        }

        ColorUtils.checkBlackBackground(this);
        ColorUtils.updateRecentsEntry(this);

        TimeUtils.setupNightTheme(this);
        UpdateUtils.checkForUpdate(this);

        if (!loaded) {
            loaded = true;
            displayConversations();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (conversationListFragment != null && !conversationListFragment.isExpanded() && !fab.isShown() &&
                        !(otherFragment instanceof ArchivedConversationListFragment) &&
                        !(otherFragment instanceof MyAccountFragment)) {
                    fab.show();
                }
            }
        }, 1000);

        navigationView.post(new Runnable() {
            @Override
            public void run() {
                snoozeIcon();
            }
        });

        if (getIntent().getBooleanExtra(EXTRA_START_MY_ACCOUNT, false)) {
            NotificationManagerCompat.from(this).cancel(SubscriptionExpirationCheckService.NOTIFICATION_ID);
            menuItemClicked(R.id.drawer_account);
        }

        if (getIntent().getData() != null && getIntent().getDataString().contains("https://messenger.klinkerapps.com/")) {
            try {
                displayShortcutConversation(Long.parseLong(getIntent().getData().getLastPathSegment()));
            } catch (Exception e) {
                e.printStackTrace();
            }

            getIntent().setData(null);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        MessengerAppWidgetProvider.refreshWidget(this);

        NotificationService.CONVERSATION_ID_OPEN = 0L;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (downloadReceiver != null) {
            unregisterReceiver(downloadReceiver);
        }
    }

    private void requestPermissions() {
        if (PermissionsUtils.checkRequestMainPermissions(this)) {
            PermissionsUtils.startMainPermissionRequest(this);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        if (Account.get(this).primary && !PermissionsUtils.isDefaultSmsApp(this)) {
            PermissionsUtils.setDefaultSmsApp(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        try {
            PermissionsUtils.processPermissionRequest(this, requestCode, permissions, grantResults);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (requestCode == REQUEST_CALL_PERMISSION) {
                callContact();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkInitialStart() {
        return Settings.get(this).firstStart;
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (actionBar != null && drawerLayout != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        searchView = (MaterialSearchView) findViewById(R.id.search_view);
        searchView.setVoiceSearch(false);
        searchView.setBackgroundColor(getResources().getColor(R.color.drawerBackground));
        searchView.setOnQueryTextListener(this);
        searchView.setOnSearchViewListener(this);
    }

    private void initDrawer() {
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.postDelayed(new Runnable() {
            @Override
            public void run() {
                Account account = Account.get(getApplicationContext());
                try {
                    ((TextView) findViewById(R.id.drawer_header_my_name))
                            .setText(account.myName);
                    ((TextView) findViewById(R.id.drawer_header_my_phone_number))
                            .setText(PhoneNumberUtils.format(account.myPhoneNumber));

                    // change the text to
                    if (Account.get(MessengerActivity.this).accountId == null) {
                        navigationView.getMenu().findItem(R.id.drawer_account).setTitle(R.string.menu_device_texting);
                    }

                    initSnooze();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
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
                        new ApiUtils().updateSnooze(Account.get(getApplicationContext()).accountId,
                                snoozeTil);

                        snoozeIcon();

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

    private void configureGlobalColors() {
        final Settings settings = Settings.get(this);
        if (settings.useGlobalThemeColor) {
            toolbar.setBackgroundColor(settings.globalColorSet.color);
            fab.setBackgroundTintList(ColorStateList.valueOf(settings.globalColorSet.colorAccent));

            int[][] states = new int[][] {
                    new int[] {-android.R.attr.state_checked },
                    new int[] { android.R.attr.state_checked }
            };

            String baseColor = getResources().getBoolean(R.bool.is_night) ? "FFFFFF" : "000000";
            int[] iconColors = new int[] {
                    Color.parseColor("#77" + baseColor),
                    settings.globalColorSet.colorAccent
            };

            int[] textColors = new int[] {
                    Color.parseColor("#DD" + baseColor),
                    settings.globalColorSet.colorAccent
            };

            navigationView.setItemIconTintList(new ColorStateList(states, iconColors));
            navigationView.setItemTextColor(new ColorStateList(states, textColors));
            navigationView.post(new Runnable() {
                @Override
                public void run() {
                    ColorUtils.adjustStatusBarColor(settings.globalColorSet.colorDark, MessengerActivity.this);

                    View header = navigationView.findViewById(R.id.header);
                    if (header != null) {
                        header.setBackgroundColor(settings.globalColorSet.colorDark);
                    }
                }
            });
        }
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

        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
            return;
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
        
        if (item == null || item.isChecked()) {
            return true;
        }

        if (item.isCheckable()) {
            item.setChecked(true);
        }

        if (drawerLayout != null) {
            if (item.getItemId() == R.id.drawer_conversation) {
                setTitle(R.string.app_title);
            } else if (item.isCheckable()) {
                setTitle(StringUtils.titleize(item.getTitle().toString()));
            }
        }

        return menuItemClicked(item.getItemId());
    }

    public boolean menuItemClicked(int id) {
        switch (id) {
            case R.id.drawer_conversation:
                if (conversationListFragment != null && conversationListFragment.archiveSnackbar != null
                        && conversationListFragment.archiveSnackbar.isShown()) {
                    conversationListFragment.archiveSnackbar.dismiss();
                }
                return displayConversations();
            case R.id.drawer_archived:
                if (conversationListFragment != null && conversationListFragment.archiveSnackbar != null
                        && conversationListFragment.archiveSnackbar.isShown()) {
                    conversationListFragment.archiveSnackbar.dismiss();
                }
                return displayArchived();
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
            case R.id.menu_call:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                        == PackageManager.PERMISSION_GRANTED) {
                    return callContact();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
                        return false;
                    } else {
                        return callContact();
                    }
                }
            case R.id.menu_view_contact:
            case R.id.drawer_view_contact:
                return viewContact();
            case R.id.menu_view_media:
            case R.id.drawer_view_media:
                return viewMedia();
            case R.id.menu_delete_conversation:
            case R.id.drawer_delete_conversation:
                return deleteConversation();
            case R.id.menu_archive_conversation:
            case R.id.drawer_archive_conversation:
                return archiveConversation();
            case R.id.menu_conversation_information:
            case R.id.drawer_conversation_information:
                return conversationInformation();
            case R.id.menu_conversation_blacklist:
            case R.id.drawer_conversation_blacklist:
                return conversationBlacklist();
            case R.id.menu_conversation_schedule:
            case R.id.drawer_conversation_schedule:
                return conversationSchedule();
            case R.id.menu_contact_settings:
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

            MenuItem item = menu.findItem(R.id.menu_search);
            searchView.setMenuItem(item);
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

        try {
            getIntent().putExtra(EXTRA_CONVERSATION_ID, conversationListFragment.getExpandedId());
        } catch (Exception e) {
            e.printStackTrace();
        }
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
     * conversation list drawer options                             *
     *****************************************************************/

    public boolean displayConversations() {
        NotificationService.CONVERSATION_ID_OPEN = 0L;

        fab.show();
        invalidateOptionsMenu();
        inSettings = false;

        long convoId = getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1L);
        long messageId = getIntent().getLongExtra(EXTRA_MESSAGE_ID, -1L);

        if (messageId != -1L && convoId != -1L) {
            conversationListFragment = ConversationListFragment.newInstance(convoId, messageId);
            getIntent().putExtra(EXTRA_CONVERSATION_ID, -1L);
            getIntent().putExtra(EXTRA_MESSAGE_ID, -1L);
        } else if (convoId != -1L && convoId != 0) {
            conversationListFragment = ConversationListFragment.newInstance(convoId);
        } else {
            conversationListFragment = ConversationListFragment.newInstance();
        }
        
        otherFragment = null;
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

    private void displayShortcutConversation(long convo) {
        NotificationService.CONVERSATION_ID_OPEN = 0L;

        fab.show();
        invalidateOptionsMenu();
        inSettings = false;

        conversationListFragment = ConversationListFragment.newInstance(convo);

        otherFragment = null;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.conversation_list_container, conversationListFragment);

        Fragment messageList = getSupportFragmentManager()
                .findFragmentById(R.id.message_list_container);

        if (messageList != null) {
            transaction.remove(messageList);
        }

        transaction.commit();
    }

    private boolean displayArchived() {
        return displayFragmentWithBackStack(new ArchivedConversationListFragment());
    }

    private boolean displaySearchFragment() {
        otherFragment = null;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.conversation_list_container, searchFragment)
                .commit();
        return true;
    }

    private boolean displayScheduledMessages() {
        return displayFragmentWithBackStack(new ScheduledMessagesFragment());
    }

    private boolean displayBlacklist() {
        return displayFragmentWithBackStack(BlacklistFragment.newInstance());
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

    private boolean displayFragmentWithBackStack(final Fragment fragment) {
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        }

        fab.hide();
        invalidateOptionsMenu();
        inSettings = true;

        otherFragment = fragment;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.conversation_list_container, fragment)
                        .commit();
            }
        }, 200);


        return true;
    }

    /*****************************************************************
     * message list drawer options                                  *
     *****************************************************************/

    public boolean callContact() {
        if (conversationListFragment.isExpanded()) {
            String uri = "tel:" +
                    conversationListFragment.getExpandedItem().conversation.phoneNumbers;
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse(uri));
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.no_apps_found, Toast.LENGTH_SHORT).show();
            } catch (SecurityException e) {
                Toast.makeText(this, R.string.you_denied_permission, Toast.LENGTH_SHORT).show();
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean viewContact() {
        if (conversationListFragment.isExpanded()) {
            Conversation conversation = conversationListFragment.getExpandedItem().conversation;
            String[] names = ContactUtils.findContactNames(conversation.phoneNumbers, this).split(", ");
            String[] numbers = conversation.phoneNumbers.split(", ");
            List<Conversation> conversations = new ArrayList<>();

            for (int i = 0; i < numbers.length; i++) {
                Conversation c = new Conversation();
                c.title = names[i];
                c.phoneNumbers = numbers[i];
                c.imageUri = ContactUtils.findImageUri(numbers[i], this);

                if (c.imageUri != null && ImageUtils.getContactImage(c.imageUri, this) == null) {
                    c.imageUri = null;
                }

                conversations.add(c);
            }

            ContactAdapter adapter = new ContactAdapter(conversations, new ContactClickedListener() {
                @Override
                public void onClicked(String title, String phoneNumber, String imageUri) {
                    Intent intent;

                    try {
                        intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,
                                String.valueOf(ContactUtils.findContactId(phoneNumber,
                                        MessengerActivity.this)));
                        intent.setData(uri);
                    } catch (NoSuchElementException e) {
                        e.printStackTrace();
                        try {
                            intent = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT);
                            intent.setData(Uri.parse("tel:" + phoneNumber));
                        } catch (ActivityNotFoundException ex) {
                            intent = null;
                        }
                    }

                    if (intent != null) {
                        startActivity(intent);
                    }
                }
            });

            RecyclerView recyclerView = new RecyclerView(this);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);

            if (adapter.getItemCount() == 1) {
                Intent intent;

                try {
                    intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,
                            String.valueOf(ContactUtils.findContactId(conversation.phoneNumbers,
                                    MessengerActivity.this)));
                    intent.setData(uri);
                } catch (NoSuchElementException e) {
                    e.printStackTrace();
                    try {
                        intent = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT);
                        intent.setData(Uri.parse("tel:" + conversation.phoneNumbers));
                    } catch (ActivityNotFoundException ex) {
                        intent = null;
                    }
                }

                if (intent != null) {
                    startActivity(intent);
                }
            } else {
                new AlertDialog.Builder(this)
                        .setView(recyclerView)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean viewMedia() {
        if (conversationListFragment.isExpanded()) {
            Intent intent = new Intent(this, MediaGridActivity.class);
            intent.putExtra(MediaGridActivity.EXTRA_CONVERSATION_ID, conversationListFragment.getExpandedId());
            startActivity(intent);

            return true;
        } else {
            return false;
        }
    }

    private boolean isArchiveConvoShowing() {
        return otherFragment != null && otherFragment instanceof ArchivedConversationListFragment &&
                ((ArchivedConversationListFragment) otherFragment).isExpanded();
    }

    private ConversationListFragment getShownConversationList() {
        return isArchiveConvoShowing() ? (ArchivedConversationListFragment) otherFragment : conversationListFragment;
    }

    private boolean deleteConversation() {
        if (conversationListFragment.isExpanded() || isArchiveConvoShowing()) {
            final ConversationListFragment fragment = getShownConversationList();
            final long conversationId = fragment.getExpandedId();
            fragment.onBackPressed();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ConversationListAdapter adapter = fragment.getAdapter();
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

    private boolean archiveConversation() {
        if (conversationListFragment.isExpanded() || isArchiveConvoShowing()) {
            final ConversationListFragment fragment = getShownConversationList();
            final long conversationId = fragment.getExpandedId();
            fragment.onBackPressed();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ConversationListAdapter adapter = fragment.getAdapter();
                    int position = adapter.findPositionForConversationId(conversationId);
                    if (position != -1) {
                        adapter.archiveItem(position);
                    }
                }
            }, 250);

            return true;
        } else {
            return false;
        }
    }

    private boolean conversationInformation() {
        if (conversationListFragment.isExpanded() || isArchiveConvoShowing()) {
            final ConversationListFragment fragment = getShownConversationList();
            Conversation conversation = fragment.getExpandedItem().conversation;
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

    private boolean conversationBlacklist() {
        if (conversationListFragment.isExpanded() || isArchiveConvoShowing()) {
            final ConversationListFragment fragment = getShownConversationList();
            Conversation conversation = fragment.getExpandedItem().conversation;
            fragment.getExpandedItem().itemView.performClick();
            clickNavigationItem(R.id.drawer_mute_contacts);
            return displayFragmentWithBackStack(
                    BlacklistFragment.newInstance(conversation.phoneNumbers));
        } else {
            return false;
        }
    }

    private boolean conversationSchedule() {
        if (conversationListFragment.isExpanded() || isArchiveConvoShowing()) {
            final ConversationListFragment fragment = getShownConversationList();
            Conversation conversation = fragment.getExpandedItem().conversation;
            fragment.getExpandedItem().itemView.performClick();
            clickNavigationItem(R.id.drawer_schedule);
            return displayFragmentWithBackStack(
                    ScheduledMessagesFragment.newInstance(conversation.title,
                            conversation.phoneNumbers));
        } else {
            return false;
        }
    }

    private boolean contactSettings() {
        if (conversationListFragment.isExpanded() || isArchiveConvoShowing()) {
            final ConversationListFragment fragment = getShownConversationList();
            long conversationId = fragment.getExpandedId();
            Intent intent = new Intent(this, ContactSettingsActivity.class);
            intent.putExtra(ContactSettingsActivity.EXTRA_CONVERSATION_ID, conversationId);
            startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.message_list_container);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        } else {
            fragment = getSupportFragmentManager().findFragmentById(R.id.conversation_list_container);
            if (fragment != null) {
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }

        if (requestCode == REQUEST_ONBOARDING) {
            boolean hasTelephone = getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

            // if it isn't a phone, then we want to force the login.
            // if it is a phone, they can choose to log in when they want to
            Intent login = new Intent(this, InitialLoadActivity.class);
            login.putExtra(LoginActivity.ARG_SKIP_LOGIN, hasTelephone);

            startActivity(login);
            finish();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        ensureSearchFragment();
        searchFragment.search(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText.length() > 0) {
            // display search fragment
            ensureSearchFragment();
            searchFragment.search(newText);
            if (!searchFragment.isAdded()) {
                displaySearchFragment();
            }
        } else {
            // display conversation fragment
            ensureSearchFragment();
            searchFragment.search(null);

            if (!conversationListFragment.isAdded()) {
                displayConversations();
                fab.hide();
            }
        }

        return true;
    }

    @Override
    public void onSearchViewShown() {
        fab.hide();
        ensureSearchFragment();
    }

    @Override
    public void onSearchViewClosed() {
        ensureSearchFragment();

        if (!searchFragment.isSearching()) {
            fab.show();

            if (!conversationListFragment.isAdded()) {
                displayConversations();
            }
        }
    }

    private void ensureSearchFragment() {
        if (searchFragment == null) {
            searchFragment = SearchFragment.newInstance();
        }
    }

    private void dismissIfFromNotification() {
        boolean fromNotification = getIntent().getBooleanExtra(EXTRA_FROM_NOTIFICATION, false);
        long convoId = getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1L);

        if (fromNotification && convoId != -1) {
            new ApiUtils().dismissNotification(Account.get(this).accountId, Account.get(this).deviceId, convoId);
        }
    }

    private void snoozeIcon() {
        boolean currentlySnoozed = Settings.get(getApplicationContext()).snooze >
                System.currentTimeMillis();
        ImageButton snooze = (ImageButton) findViewById(R.id.snooze);

        if (snooze != null) {
            if (currentlySnoozed) {
                snooze.setImageResource(R.drawable.ic_snoozed);
            } else {
                snooze.setImageResource(R.drawable.ic_snooze);
            }
        }
    }

    public void startDataDownload() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                downloadReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        recreate();
                    }
                };

                registerReceiver(downloadReceiver,
                        new IntentFilter(ApiDownloadService.ACTION_DOWNLOAD_FINISHED));

                startService(new Intent(MessengerActivity.this, ApiDownloadService.class));
            }
        }, 1000);

        Snackbar.make(findViewById(android.R.id.content), R.string.downloading_and_decrypting, Snackbar.LENGTH_LONG).show();
    }
}
