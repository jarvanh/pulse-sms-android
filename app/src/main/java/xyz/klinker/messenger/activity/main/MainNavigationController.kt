package xyz.klinker.messenger.activity.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.fragment.conversation.ConversationListFragment
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.service.ApiDownloadService
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.PhoneNumberUtils
import xyz.klinker.messenger.shared.util.StringUtils
import xyz.klinker.messenger.shared.util.listener.BackPressedListener

@Suppress("DEPRECATION")
class MainNavigationController(private val activity: MessengerActivity)
    : NavigationView.OnNavigationItemSelectedListener {

    val conversationActionDelegate = MainNavigationConversationListActionDelegate(activity)
    val messageActionDelegate = MainNavigationMessageListActionDelegate(activity)

    private val navigationView: NavigationView by lazy { activity.findViewById<View>(R.id.navigation_view) as NavigationView }
    private val drawerLayout: DrawerLayout? by lazy { activity.findViewById<View>(R.id.drawer_layout) as DrawerLayout? }

    var conversationListFragment: ConversationListFragment? = null
    var otherFragment: Fragment? = null
    var inSettings = false
    var selectedNavigationItemId: Int = R.id.drawer_conversation

    fun isConversationListExpanded() = conversationListFragment != null && conversationListFragment!!.isExpanded
    fun isOtherFragmentConvoAndShowing() = otherFragment != null && otherFragment is ConversationListFragment && (otherFragment as ConversationListFragment).isExpanded
    fun getShownConversationList() = when {
        isOtherFragmentConvoAndShowing() -> otherFragment as ConversationListFragment
        else -> conversationListFragment
    }

    fun initDrawer() {
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.postDelayed({
            try {
                if (Account.exists()) {
                    (activity.findViewById<View>(R.id.drawer_header_my_name) as TextView).text = Account.myName
                }

                (activity.findViewById<View>(R.id.drawer_header_my_phone_number) as TextView).text =
                        PhoneNumberUtils.format(PhoneNumberUtils.getMyPhoneNumber(activity))

                if (!ColorUtils.isColorDark(Settings.mainColorSet.colorDark)) {
                    (activity.findViewById<View>(R.id.drawer_header_my_name) as TextView)
                            .setTextColor(activity.resources.getColor(R.color.lightToolbarTextColor))
                    (activity.findViewById<View>(R.id.drawer_header_my_phone_number) as TextView)
                            .setTextColor(activity.resources.getColor(R.color.lightToolbarTextColor))
                }

                // change the text to
                if (!Account.exists()) {
                    navigationView.menu.findItem(R.id.drawer_account).setTitle(R.string.menu_device_texting)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            activity.snoozeController.initSnooze()
        }, 300)
    }

    fun initToolbarTitleClick() {
        activity.toolbar.setOnClickListener {
            val otherFrag = otherFragment
            val fragment = when {
                conversationListFragment != null -> conversationListFragment
                otherFrag is ConversationListFragment -> otherFrag
                else -> return@setOnClickListener
            }

            fragment?.recyclerView?.smoothScrollToPosition(0)
        }
    }

    fun openDrawer(): Boolean {
        if (drawerLayout != null && !drawerLayout!!.isDrawerOpen(GravityCompat.START)) {
            drawerLayout!!.openDrawer(GravityCompat.START)
            return true
        }

        return false
    }

    fun closeDrawer(): Boolean {
        if (drawerLayout != null && drawerLayout!!.isDrawerOpen(GravityCompat.START)) {
            drawerLayout!!.closeDrawer(GravityCompat.START)
            return true
        }

        return false
    }

    fun backPressed(): Boolean {
        val fragments = activity.supportFragmentManager.fragments

        fragments
                .filter { it is BackPressedListener && (it as BackPressedListener).onBackPressed() }
                .forEach { return true }

        when {
            conversationListFragment == null -> {
                val messageListFragment = activity.supportFragmentManager.findFragmentById(R.id.message_list_container)

                if (messageListFragment != null) {
                    try {
                        activity.supportFragmentManager.beginTransaction().remove(messageListFragment).commit()
                    } catch (e: Exception) {
                    }
                }

                conversationActionDelegate.displayConversations()
                activity.fab.show()
                drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                return true
            }
            inSettings -> {
                onNavigationItemSelected(R.id.drawer_conversation)
                return true
            }
            else -> return false
        }
    }

    fun drawerItemClicked(id: Int): Boolean {
        conversationListFragment?.swipeHelper?.dismissSnackbars()

        when (id) {
            R.id.drawer_conversation -> return conversationActionDelegate.displayConversations()
            R.id.drawer_archived -> return conversationActionDelegate.displayArchived()
            R.id.drawer_private -> return conversationActionDelegate.displayPrivate()
            R.id.drawer_unread -> return conversationActionDelegate.displayUnread()
            R.id.drawer_schedule -> return conversationActionDelegate.displayScheduledMessages()
            R.id.drawer_mute_contacts -> return conversationActionDelegate.displayBlacklist()
            R.id.drawer_invite -> return conversationActionDelegate.displayInviteFriends()
            R.id.drawer_feature_settings -> return conversationActionDelegate.displayFeatureSettings()
            R.id.drawer_settings -> return conversationActionDelegate.displaySettings()
            R.id.drawer_account -> return conversationActionDelegate.displayMyAccount()
            R.id.drawer_help -> return conversationActionDelegate.displayHelpAndFeedback()
            R.id.drawer_about -> return conversationActionDelegate.displayAbout()
            R.id.drawer_edit_folders -> return conversationActionDelegate.displayEditFolders()
            R.id.menu_view_contact, R.id.drawer_view_contact -> return messageActionDelegate.viewContact()
            R.id.menu_view_media, R.id.drawer_view_media -> return messageActionDelegate.viewMedia()
            R.id.menu_delete_conversation, R.id.drawer_delete_conversation -> return messageActionDelegate.deleteConversation()
            R.id.menu_archive_conversation, R.id.drawer_archive_conversation -> return messageActionDelegate.archiveConversation()
            R.id.menu_conversation_information, R.id.drawer_conversation_information -> return messageActionDelegate.conversationInformation()
            R.id.menu_conversation_blacklist, R.id.drawer_conversation_blacklist -> return messageActionDelegate.conversationBlacklist()
            R.id.menu_conversation_schedule, R.id.drawer_conversation_schedule -> return messageActionDelegate.conversationSchedule()
            R.id.menu_contact_settings, R.id.drawer_contact_settings -> return messageActionDelegate.contactSettings()
            R.id.menu_call -> return if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                messageActionDelegate.callContact()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), MessengerActivityExtras.REQUEST_CALL_PERMISSION)
                    false
                } else {
                    messageActionDelegate.callContact()
                }
            }
            else -> {
                val folder = activity.drawerItemHelper.findFolder(id)
                return if (folder != null) {
                    conversationActionDelegate.displayFolder(folder)
                } else {
                    true
                }
            }
        }
    }

    fun optionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            openDrawer()
            true
        }
        R.id.menu_search -> true
        else -> false
    }

    fun onNavigationItemSelected(itemId: Int) {
        val item = navigationView.menu.findItem(itemId)
        if (item != null) {
            onNavigationItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        closeDrawer()
        selectedNavigationItemId = item.itemId

        if (item.isChecked || ApiDownloadService.IS_RUNNING) {
            return true
        }

        if (item.isCheckable) {
            item.isChecked = true
        }

        if (item.itemId == R.id.drawer_conversation) {
            activity.setTitle(R.string.app_title)
        } else if (item.isCheckable) {
            activity.title = StringUtils.titleize(item.title.toString())
        }

        return drawerItemClicked(item.itemId)
    }
}