package xyz.klinker.messenger.shared.util

import android.support.design.widget.NavigationView
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.FeatureFlags

class DrawerItemHelper(private val navigationView: NavigationView) {

    fun prepareDrawer() {
        addFolders()
        removeItemsBasedOnFeatureToggles()
    }

    /**
     * Return the index of the clicked folder, otherwise null, if not a folder
     */
    fun tryFolderClick(itemId: Int): Pair<Int, Long>? {
        // TODO: iterate through the folders and see if their item id matches the provided one.
        // if it does, return a pair with the index of the item, and a the folder id

        return null
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