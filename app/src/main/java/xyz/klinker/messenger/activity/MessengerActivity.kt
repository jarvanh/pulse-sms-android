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

package xyz.klinker.messenger.activity

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.compose.ComposeActivity
import xyz.klinker.messenger.activity.main.*
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.AnimationUtils
import xyz.klinker.messenger.shared.util.PromotionUtils
import xyz.klinker.messenger.shared.util.UnreadBadger
import xyz.klinker.messenger.shared.view.WhitableToolbar
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider
import xyz.klinker.messenger.utils.UpdateUtils
import android.view.inputmethod.InputMethodManager
import xyz.klinker.messenger.fragment.PrivateConversationListFragment
import xyz.klinker.messenger.fragment.conversation.ConversationListFragment
import xyz.klinker.messenger.shared.util.DrawerItemHelper


/**
 * Main entry point to the app. This will serve for setting up the drawer view, finding
 * conversations and displaying things on the screen to get the user started.
 */
class MessengerActivity : AppCompatActivity() {

    val navController = MainNavigationController(this)
    val accountController = MainAccountController(this)
    val intentHandler = MainIntentHandler(this)
    val searchHelper = MainSearchHelper(this)
    val snoozeController = SnoozeController(this)
    private val colorController = MainColorController(this)
    private val startDelegate = MainOnStartDelegate(this)
    private val permissionHelper = MainPermissionHelper(this)
    private val resultHandler = MainResultHandler(this)

    val drawerItemHelper: DrawerItemHelper by lazy { DrawerItemHelper(findViewById(R.id.navigation_view)) }

    val toolbar: WhitableToolbar by lazy { findViewById<View>(R.id.toolbar) as WhitableToolbar }
    val fab: FloatingActionButton by lazy { findViewById<View>(R.id.fab) as FloatingActionButton }
    private val content: View by lazy { findViewById<View>(android.R.id.content) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UpdateUtils(this).checkForUpdate()

        setContentView(R.layout.activity_messenger)

        initToolbar()
        fab.setOnClickListener { startActivity(Intent(applicationContext, ComposeActivity::class.java)) }

        colorController.configureGlobalColors()
        colorController.configureNavigationBarColor()
        intentHandler.dismissIfFromNotification()
        navController.conversationActionDelegate.displayConversations(savedInstanceState)
        navController.initToolbarTitleClick()
        accountController.startIntroOrLogin(savedInstanceState)
        permissionHelper.requestDefaultSmsApp()

        val content = findViewById<View>(R.id.content)
        content.post {
            AnimationUtils.conversationListSize = content.height
            AnimationUtils.toolbarSize = toolbar.height
        }

        drawerItemHelper.prepareDrawer()
    }

    public override fun onStart() {
        super.onStart()

        PromotionUtils(this).checkPromotions()
        UnreadBadger(this).clearCount()

        colorController.colorActivity()
        navController.initDrawer()
        intentHandler.displayAccount()
        intentHandler.handleShortcutIntent(intent)
        accountController.listenForFullRefreshes()
        accountController.refreshAccountToken()

        startDelegate.run()

        try {
            if (navController.otherFragment is PrivateConversationListFragment) {
                if (navController.isOtherFragmentConvoAndShowing()) {
                    navController.getShownConversationList()?.expandedItem?.itemView?.performClick()
                }

                clickNavigationItem(R.id.drawer_conversation)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentHandler.newIntent(intent)
    }


    public override fun onPause() {
        super.onPause()

        if (navController.conversationListFragment != null) {
            navController.conversationListFragment!!.swipeHelper.dismissSnackbars()
        }
    }

    public override fun onStop() {
        super.onStop()

        MessengerAppWidgetProvider.refreshWidget(this)
        accountController.stopListeningForRefreshes()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(content.windowToken, 0)
    }

    public override fun onSaveInstanceState(outState: Bundle?) {
        var outState = intentHandler.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    public override fun onDestroy() {
        super.onDestroy()
        accountController.stopListeningForDownloads()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.handlePermissionResult(requestCode, permissions, grantResults)
    }

    override fun onBackPressed() {
        try {
            if (navController.closeDrawer()) {
            } else if (searchHelper.closeSearch()) {
            } else if (!navController.backPressed()) {
                super.onBackPressed()
            }
        } catch (e: Exception) {

        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!navController.inSettings) {
            menuInflater.inflate(R.menu.activity_messenger, menu)

            val item = menu.findItem(R.id.menu_search)
            item.icon.setTintList(ColorStateList.valueOf(toolbar.textColor))
            searchHelper.setup(item)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return navController.optionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            Settings.forceUpdate(this)
            colorController.colorActivity()
            colorController.configureGlobalColors()

            resultHandler.handle(requestCode, resultCode, data)
            accountController.startLoad(requestCode)
            super.onActivityResult(requestCode, resultCode, data)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun clickNavigationItem(itemId: Int) { navController.onNavigationItemSelected(itemId) }
    fun displayConversations() = navController.conversationActionDelegate.displayConversations()

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar

        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
    }
}
