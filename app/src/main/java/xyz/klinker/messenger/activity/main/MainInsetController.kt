package xyz.klinker.messenger.activity.main

import android.annotation.SuppressLint
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.fragment.BlacklistFragment
import xyz.klinker.messenger.fragment.ScheduledMessagesFragment
import xyz.klinker.messenger.fragment.conversation.ConversationListFragment
import xyz.klinker.messenger.fragment.message.MessageListFragment
import xyz.klinker.messenger.shared.util.AndroidVersionUtil
import xyz.klinker.messenger.shared.util.DensityUtil

class MainInsetController(private val activity: MessengerActivity) {

    private val sixteenDp: Int by lazy { DensityUtil.toDp(activity, 16) }
    private var bottomInsetValue: Int = 0

    fun applyWindowStatusFlags() {
        if (!AndroidVersionUtil.isAndroidQ) {
            return
        }

        val oldSystemUiFlags = activity.window.decorView.systemUiVisibility
        val newSystemUiFlags = oldSystemUiFlags or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        activity.window.decorView.systemUiVisibility = newSystemUiFlags
    }

    @SuppressLint("RestrictedApi")
    fun overrideDrawerInsets() {
        if (!AndroidVersionUtil.isAndroidQ) {
            return
        }

        activity.navController.drawerLayout?.setOnApplyWindowInsetsListener { _, insets ->
            if (insets.systemWindowInsetBottom != 0 && bottomInsetValue == 0) {
                bottomInsetValue = insets.systemWindowInsetBottom
            }

            val modifiedInsets = insets.replaceSystemWindowInsets(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, 0)
            activity.navController.drawerLayout?.setChildInsets(modifiedInsets, insets.systemWindowInsetTop > 0)

            modifyMessengerActivityElements()
            modifyConversationListElements(activity.navController.conversationListFragment)

            modifiedInsets
        }
    }

    fun modifyConversationListElements(fragment: ConversationListFragment?) {
        if (!AndroidVersionUtil.isAndroidQ || fragment == null) {
            return
        }

        val recycler = fragment.recyclerView
        recycler.clipToPadding = false
        recycler.setPadding(recycler.paddingLeft, recycler.paddingTop, recycler.paddingRight, recycler.paddingBottom + bottomInsetValue)
    }

    fun modifyScheduledMessageElements(fragment: ScheduledMessagesFragment) {
        if (!AndroidVersionUtil.isAndroidQ) {
            return
        }

        val recycler = fragment.list
        recycler.clipToPadding = false
        recycler.setPadding(recycler.paddingLeft, recycler.paddingTop, recycler.paddingRight, recycler.paddingBottom + bottomInsetValue)

        // move fab above the nav bar
        val params = fragment.fab.layoutParams as FrameLayout.LayoutParams
        params.bottomMargin = sixteenDp + bottomInsetValue
    }

    fun modifyBlacklistElements(fragment: BlacklistFragment) {
        if (!AndroidVersionUtil.isAndroidQ) {
            return
        }

        val recycler = fragment.list
        recycler.clipToPadding = false
        recycler.setPadding(recycler.paddingLeft, recycler.paddingTop, recycler.paddingRight, recycler.paddingBottom + bottomInsetValue)

        // move fab above the nav bar
        val params = fragment.fab.layoutParams as FrameLayout.LayoutParams
        params.bottomMargin = sixteenDp + bottomInsetValue
    }

    fun modifyMessageListElements(fragment: MessageListFragment) {
        if (!AndroidVersionUtil.isAndroidQ) {
            return
        }

        val sendbar = fragment.nonDeferredInitializer.replyBarCard.getChildAt(0)
        sendbar.setPadding(sendbar.paddingLeft, sendbar.paddingTop, sendbar.paddingRight, sendbar.paddingBottom + bottomInsetValue)
    }

    private fun modifyMessengerActivityElements() {
        // move fab above the nav bar
        val params = activity.fab.layoutParams as CoordinatorLayout.LayoutParams
        params.bottomMargin = sixteenDp + bottomInsetValue

        // put padding at the bottom of the navigation view's recycler view
        val navView = activity.navController.navigationView
        val navRecycler = navView.getChildAt(0) as RecyclerView
        navRecycler.clipToPadding = false
        navRecycler.setPadding(navView.paddingLeft, navView.paddingTop, navView.paddingRight, bottomInsetValue)
    }
}