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
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.compose.ComposeActivity
import xyz.klinker.messenger.activity.main.*
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.view.WhitableToolbar
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider
import xyz.klinker.messenger.shared.util.UpdateUtils
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import xyz.klinker.messenger.fragment.PrivateConversationListFragment
import xyz.klinker.messenger.fragment.settings.MyAccountFragment
import xyz.klinker.messenger.shared.data.pojo.BaseTheme
import xyz.klinker.messenger.shared.service.notification.NotificationConstants
import xyz.klinker.messenger.shared.util.*


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
    val insetController = MainInsetController(this)
    private val colorController = MainColorController(this)
    private val startDelegate = MainOnStartDelegate(this)
    private val permissionHelper = MainPermissionHelper(this)
    private val resultHandler = MainResultHandler(this)

    val drawerItemHelper: DrawerItemHelper by lazy { DrawerItemHelper(findViewById(R.id.navigation_view)) }

    val toolbar: WhitableToolbar by lazy { findViewById<View>(R.id.toolbar) as WhitableToolbar }
    val fab: FloatingActionButton by lazy { findViewById<View>(R.id.fab) as FloatingActionButton }
    val snackbarContainer: FrameLayout by lazy { findViewById<FrameLayout>(R.id.snackbar_container) }
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
        intentHandler.restoreNavigationSelection(savedInstanceState)
        navController.conversationActionDelegate.displayConversations(savedInstanceState)
        intentHandler.displayPrivateFromNotification()
        navController.initToolbarTitleClick()
        accountController.startIntroOrLogin(savedInstanceState)
        permissionHelper.requestDefaultSmsApp()
        insetController.applyWindowStatusFlags()

        val content = findViewById<View>(R.id.content)
        content.post {
            AnimationUtils.conversationListSize = content.height
            AnimationUtils.toolbarSize = toolbar.height
        }

        drawerItemHelper.prepareDrawer()

        if (Settings.baseTheme == BaseTheme.BLACK) {
            findViewById<View?>(xyz.klinker.messenger.shared.R.id.nav_bar_divider)?.visibility = View.GONE
        }
    }

    public override fun onStart() {
        super.onStart()

        PromotionUtils(this).checkPromotions {
            MyAccountFragment.openTrialUpgradePreference = true
            clickNavigationItem(R.id.drawer_account)
        }

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

    public override fun onResume() {
        super.onResume()
        insetController.onResume()

        val messageListFragment = navController.findMessageListFragment()
        if (messageListFragment != null) {
            NotificationConstants.CONVERSATION_ID_OPEN = messageListFragment.conversationId
        }
    }

    public override fun onPause() {
        super.onPause()

        if (navController.conversationListFragment != null) {
            navController.conversationListFragment!!.swipeHelper.dismissSnackbars()
        }

        insetController.onPause()
        NotificationConstants.CONVERSATION_ID_OPEN = 0L
    }

    public override fun onStop() {
        super.onStop()

        MessengerAppWidgetProvider.refreshWidget(this)
        accountController.stopListeningForRefreshes()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(content.windowToken, 0)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
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

        if (actionBar != null && !resources.getBoolean(R.bool.pin_drawer)) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
    }
}
