package xyz.klinker.messenger.shared.util

import android.support.design.widget.NavigationView
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.model.Folder

class DrawerItemHelper(private val navigationView: NavigationView) {

    fun prepareDrawer() {
        queryAndAddFolders()
        removeItemsBasedOnFeatureToggles()
    }

    /**
     * Return the index of the clicked folder and the folder id, otherwise null, if not a folder
     */
    fun tryFolderClick(itemId: Int): Folder? {
        for (i in 0 until(folders?.size ?: 0)) {
            if (folders!![i].id.toInt() == itemId) {
                return folders!![i]
            }
        }

        return null
    }

    private fun queryAndAddFolders() {
        if (!FeatureFlags.FOLDER_SUPPORT) {
            return
        }

        if (folders == null) {
            Thread {
                folders = DataSource.getFoldersAsList(navigationView.context)
                navigationView.post { addFoldersToDrawer(folders!!) }
            }.start()
        } else {
            navigationView.post { addFoldersToDrawer(folders!!) }
        }
    }

    private fun addFoldersToDrawer(folders: List<Folder>) {
        folders.forEach {
            navigationView.menu.add(R.id.primary_section, it.id.toInt(), 1, it.name).setIcon(R.drawable.ic_folder).isCheckable = true
        }

        val editItem = navigationView.menu.add(R.id.primary_section, R.id.drawer_edit_folders, 1, R.string.menu_edit_folders)
        editItem.isCheckable = true
        editItem.setIcon(R.drawable.ic_add)
    }

    private fun removeItemsBasedOnFeatureToggles() {
        if (!FeatureFlags.FOLDER_SUPPORT) {
            navigationView.menu.findItem(R.id.drawer_unread).isVisible = false
        }
    }

    companion object {
        private var folders: List<Folder>? = null
    }
}