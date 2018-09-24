package xyz.klinker.messenger.shared.util

import com.google.android.material.navigation.NavigationView
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.model.Folder

class DrawerItemHelper(private val navigationView: NavigationView) {

    fun prepareDrawer() {
        queryAndAddFolders()
        removeItemsBasedOnFeatureToggles()
    }

    fun findFolder(itemId: Int) = folders?.firstOrNull { it.id.toInt() == itemId }

    private fun queryAndAddFolders() {
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
//        if (!FeatureFlags.FOLDER_SUPPORT) {
//            navigationView.menu.findItem(R.id.drawer_unread).isVisible = false
//        }
    }

    companion object {
        var folders: List<Folder>? = null
    }
}