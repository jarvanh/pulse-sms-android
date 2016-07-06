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

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.fragment.ConversationListFragment;
import xyz.klinker.messenger.util.OnBackPressedListener;

/**
 * Main entry point to the app. This will serve for setting up the drawer view, finding
 * conversations and displaying things on the screen to get the user started.
 */
public class MessengerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_messenger);
        initToolbar();
        initDrawer();
        initFab();
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

        clickDefaultDrawerItem();
    }

    private void initFab() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();

        for (Fragment fragment : fragments) {
            if (fragment instanceof OnBackPressedListener) {
                if (((OnBackPressedListener) fragment).onBackPressed()) {
                    return;
                }
            }
        }

        super.onBackPressed();
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

        switch (item.getItemId()) {
            case R.id.drawer_conversation:  return displayConversations();
            default:                        return true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_messenger, menu);
        return true;
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

    private boolean displayConversations() {
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.conversation_list_container, ConversationListFragment.newInstance())
                .commit();

        return true;
    }

}
