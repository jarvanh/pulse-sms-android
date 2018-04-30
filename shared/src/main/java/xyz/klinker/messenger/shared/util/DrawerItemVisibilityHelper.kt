package xyz.klinker.messenger.shared.util

import android.support.design.widget.NavigationView
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.FeatureFlags

class DrawerItemVisibilityHelper(private val navigationView: NavigationView) {

    fun prepareDrawer() {
        addFolders()
        removeItemsBasedOnFeatureToggles()
    }

    private fun addFolders() {
        if (!FeatureFlags.FOLDER_SUPPORT) {
            return
        }

        // TODO: Async query what folders should be shown (or can I just hold a reference list here?)
    }

    private fun removeItemsBasedOnFeatureToggles() {
        if (!FeatureFlags.FOLDER_SUPPORT) {
            navigationView.menu.findItem(R.id.drawer_unread).isVisible = false
        }
    }
}