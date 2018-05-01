package xyz.klinker.messenger.fragment.settings

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.preference.Preference
import android.preference.PreferenceGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText

import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Folder
import xyz.klinker.messenger.shared.util.DrawerItemHelper

class FolderManagementFragment : MaterialPreferenceFragment() {

    private val foldersPrefGroup: PreferenceGroup by lazy { findPreference(getString(R.string.pref_folders_category)) as PreferenceGroup }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.settings_folder)

        fillFolderList()
    }

    private fun fillFolderList() {
        val createNewFolder = Preference(activity)
        createNewFolder.setTitle(R.string.create_folder)
        createNewFolder.setSummary(R.string.create_folder_summary)
        createNewFolder.setOnPreferenceClickListener {
            promptCreateNewFolder()
            true
        }

        foldersPrefGroup.removeAll()
        foldersPrefGroup.addPreference(createNewFolder)

        DataSource.getFoldersAsList(activity)
                .forEach {
                    val pref = createPreference(it)
                    foldersPrefGroup.addPreference(pref)
                }
    }

    private fun promptCreateNewFolder() {
        val layout = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_text, null, false)
        val editText = layout.findViewById<View>(R.id.edit_text) as EditText
        editText.setHint(R.string.folder_name)

        AlertDialog.Builder(activity)
                .setView(layout)
                .setPositiveButton(R.string.create) { _, _ ->
                    val signature = editText.text.toString()
                    createAndShowFolder(signature)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun promptEditFolder() {
        // TODO: make sure to invalidate the folder list on delete
    }

    private fun createAndShowFolder(title: String) {
        val folder = Folder()
        folder.id = DataSource.generateId()
        folder.name = title
        folder.colors = ColorSet.DEFAULT(activity)

        DataSource.insertFolder(activity, folder, true)
        fillFolderList()

        DrawerItemHelper.folders = null
    }

    private fun createPreference(folder: Folder): Preference {
        val pref = Preference(activity)
        pref.title = folder.name

        pref.setOnPreferenceClickListener {
            true
        }

        return pref
    }
}
