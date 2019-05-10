package xyz.klinker.messenger.activity.main

import android.annotation.SuppressLint
import android.view.View
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.shared.util.AndroidVersionUtil
import xyz.klinker.messenger.shared.util.DensityUtil

class MainInsetController(private val activity: MessengerActivity) {

    var bottomInsetValue: Int = 0

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
            if (insets.systemWindowInsetBottom != 0) {
                bottomInsetValue = insets.systemWindowInsetBottom
            }

            val modifiedInsets = insets.replaceSystemWindowInsets(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, 0)
            activity.navController.drawerLayout?.setChildInsets(modifiedInsets, insets.systemWindowInsetTop > 0)

            modifyMessengerActivityElements()
            modifiedInsets
        }
    }

    fun modifyConversationListElements() {

    }

    private fun modifyMessengerActivityElements() {
        val sixteenDp = DensityUtil.toDp(activity, 16)

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