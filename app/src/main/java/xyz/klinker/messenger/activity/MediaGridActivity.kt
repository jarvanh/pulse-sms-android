package xyz.klinker.messenger.activity

import android.os.Bundle
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.main.MainColorController
import xyz.klinker.messenger.fragment.ArchivedConversationListFragment
import xyz.klinker.messenger.fragment.MediaGridFragment
import xyz.klinker.messenger.shared.activity.AbstractSettingsActivity
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.util.ColorUtils

class MediaGridActivity : AbstractSettingsActivity() {

    private val fragment: MediaGridFragment by lazy { MediaGridFragment.newInstance(
            intent.getLongExtra(EXTRA_CONVERSATION_ID, -1)) }

    private val actionMode = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            menuInflater?.inflate(R.menu.action_mode_archive_list, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            menu?.removeItem(R.id.menu_archive_conversation)
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.menu_delete_conversation -> {
                    fragment.deleteSelected()
                    mode?.finish()
                }
            }

            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            fragment.destroyActionMode()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentManager.beginTransaction()
                .replace(R.id.settings_content, fragment)
                .commit()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ColorUtils.checkBlackBackground(this)
        MainColorController(this).configureNavigationBarColor()
    }

    public override fun onStart() {
        super.onStart()
        ActivityUtils.setTaskDescription(this, fragment.conversation!!.title!!, fragment.conversation!!.colors.color)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }

        return true
    }

    fun startMultiSelect() {
        startSupportActionMode(actionMode)
    }

    companion object {
        val EXTRA_CONVERSATION_ID = "conversation_id"
    }
}
