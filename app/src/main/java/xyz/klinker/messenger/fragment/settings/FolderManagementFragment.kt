package xyz.klinker.messenger.fragment.settings

import android.app.AlertDialog
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceGroup
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText

import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.ConversationsForFolderAdapter
import xyz.klinker.messenger.adapter.InviteFriendsAdapter
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Folder
import xyz.klinker.messenger.shared.util.DrawerItemHelper
import xyz.klinker.messenger.shared.util.listener.ContactClickedListener

class FolderManagementFragment : MaterialPreferenceFragment() {

    private val foldersPrefGroup: PreferenceGroup by lazy { findPreference(getString(R.string.pref_folders_category)) as PreferenceGroup }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.settings_folder)

        fillFolderList()
        findPreference(getString(R.string.pref_create_folder)).setOnPreferenceClickListener {
            promptCreateNewFolder()
            true
        }
    }

    private fun fillFolderList() {
        foldersPrefGroup.removeAll()
        val folders = DataSource.getFoldersAsList(activity)
        folders.forEach {
            val pref = createPreference(it)
            foldersPrefGroup.addPreference(pref)
        }

        if (folders.isEmpty()) {
            val createNewFolder = Preference(activity)
            createNewFolder.setSummary(R.string.no_folders)
            createNewFolder.setOnPreferenceClickListener {
                promptCreateNewFolder()
                true
            }

            foldersPrefGroup.addPreference(createNewFolder)
        }
    }

    private fun promptCreateNewFolder() {
        val layout = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_text, null, false)
        val editText = layout.findViewById<View>(R.id.edit_text) as EditText
        editText.setHint(R.string.folder_name)

        AlertDialog.Builder(activity)
                .setView(layout)
                .setPositiveButton(R.string.create) { _, _ ->
                    val title = editText.text.toString()
                    createAndShowFolder(title)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun promptEditFolder(folder: Folder) {
        val layout = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_folder, null, false)
        val editText = layout.findViewById<EditText>(R.id.edit_text)
        editText.setHint(R.string.folder_name)
        editText.setText(folder.name)
        editText.setSelection(editText.text.length)

        val recyclerView = layout.findViewById<RecyclerView>(R.id.conversation_list)
        recyclerView.layoutManager = LinearLayoutManager(activity)

        Thread {
            val conversations = DataSource.getAllConversationsAsList(activity)
            val selectedConversations = DataSource.getFolderConversationsAsList(activity, folder.id).map { it.id }.toMutableList()

            recyclerView.post {
                recyclerView.adapter = ConversationsForFolderAdapter(conversations, object : ContactClickedListener {
                    override fun onClicked(conversation: Conversation) {
                        if (selectedConversations.contains(conversation.id)) {
                            selectedConversations.remove(conversation.id)
                            DataSource.removeConversationFromFolder(activity, conversation.id, true)
                        } else {
                            selectedConversations.add(conversation.id)
                            DataSource.addConversationToFolder(activity, conversation.id, folder.id, true)
                        }
                    }
                }, selectedConversations, folder.id)
            }
        }.start()

        AlertDialog.Builder(activity)
                .setView(layout)
                .setPositiveButton(R.string.save) { _, _ ->
                    val title = editText.text.toString()
                    folder.name = title
                    DataSource.updateFolder(activity, folder, true)

                    fillFolderList()
                    DrawerItemHelper.folders = null
                }
                .setNegativeButton(R.string.delete) { _, _ ->
                    DataSource.deleteFolder(activity, folder.id, true)

                    fillFolderList()
                    DrawerItemHelper.folders = null
                }
                .setNeutralButton(R.string.cancel, null)
                .show()
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
            promptEditFolder(folder)
            true
        }

        return pref
    }
}
