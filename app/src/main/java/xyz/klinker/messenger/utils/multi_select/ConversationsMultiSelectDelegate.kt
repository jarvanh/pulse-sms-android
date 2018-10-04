package xyz.klinker.messenger.utils.multi_select

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.SelectableHolder
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder
import xyz.klinker.messenger.fragment.ArchivedConversationListFragment
import xyz.klinker.messenger.fragment.conversation.ConversationListFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.pojo.BaseTheme
import java.util.*
import android.graphics.PorterDuff
import android.util.Log

@Suppress("DEPRECATION")
class ConversationsMultiSelectDelegate(private val fragment: ConversationListFragment) : MultiSelector() {

    private val activity: AppCompatActivity? by lazy { fragment.activity as AppCompatActivity? }
    private var adapter: ConversationListAdapter? = null

    private var mode: ActionMode? = null
    private val actionMode = object : ModalMultiSelectorCallback(this) {
        override fun onCreateActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
            super.onCreateActionMode(actionMode, menu)

            if (fragment is ArchivedConversationListFragment) {
                activity?.menuInflater?.inflate(R.menu.action_mode_archive_list, menu)
            } else {
                activity?.menuInflater?.inflate(R.menu.action_mode_conversation_list, menu)
            }

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val delete = menu.findItem(R.id.menu_delete_conversation)
            val archive = menu.findItem(R.id.menu_archive_conversation)
            val unread = menu.findItem(R.id.menu_mark_as_unread)

            changeMenuItemColor(delete)
            changeMenuItemColor(archive)

            var checked = 0
            for (i in 0 until mSelections.size()) {
                val key = mSelections.keyAt(i)
                if (mSelections.get(key))
                    checked++
                if (checked > 1)
                    break
            }

            if (checked == 0) {
                clearActionMode()
            }

            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            clearSelections()

            // https://github.com/bignerdranch/recyclerview-multiselect/issues/9#issuecomment-140180348
            try {
                val field = this@ConversationsMultiSelectDelegate.javaClass.getDeclaredField("mIsSelectable")
                if (field != null) {
                    if (!field.isAccessible)
                        field.isAccessible = true
                    field.set(this, false)
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
            }

            Handler().postDelayed({ isSelectable = false }, 250)
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            var handled = false

            val selectedPositions = ArrayList<Int>()
            val selectedConversations = ArrayList<Conversation>()
            for (i in 0 until adapter!!.itemCount) {
                if (isSelected(i, 0)) {
                    selectedPositions.add(i)

                    try {
                        if (adapter!!.showHeaderAboutTextingOnline()) {
                            selectedConversations.add(adapter!!.findConversationForPosition(i - 1))
                        } else {
                            selectedConversations.add(adapter!!.findConversationForPosition(i))
                        }
                    } catch (e: ArrayIndexOutOfBoundsException) {

                    }

                }
            }

            val source = DataSource

            when (item.itemId) {
                R.id.menu_archive_conversation -> {
                    handled = true

                    var removed = 0
                    run {
                        var i = 0
                        while (i < adapter!!.itemCount) {
                            if (isSelected(i + removed, 0)) {
                                val removedHeader = adapter!!.archiveItem(i)
                                removed += if (removedHeader) 2 else 1
                                i--
                            }
                            i++
                        }
                    }
                }
                R.id.menu_delete_conversation -> {
                    handled = true

                    var removed = 0
                    var i = 0
                    while (i < adapter!!.itemCount) {
                        if (isSelected(i + removed, 0)) {
                            val removedHeader = adapter!!.deleteItem(i)
                            removed += if (removedHeader) 2 else 1
                            i--
                        }

                        i++
                    }
                }
                R.id.menu_mute_conversation -> {
                    handled = true

                    for (conversation in selectedConversations) {
                        conversation.mute = !conversation.mute
                        source.updateConversationSettings(activity!!, conversation)
                    }

                    fragment.recyclerManager.loadConversations()
                }
                R.id.menu_pin_conversation -> {
                    handled = true

                    for (conversation in selectedConversations) {
                        conversation.pinned = !conversation.pinned
                        source.updateConversationSettings(activity!!, conversation)
                    }

                    fragment.recyclerManager.loadConversations()
                }
                R.id.menu_mark_as_unread -> {
                    handled = true

                    for (conversation in selectedConversations) {
                        conversation.read = false
                        source.markConversationAsUnread(activity!!, conversation.id, true)
                    }

                    fragment.recyclerManager.loadConversations()
                }
            }

            mode.finish()
            return handled
        }
    }

    fun setAdapter(adapter: ConversationListAdapter) {
        this.adapter = adapter
    }

    fun startActionMode() {
        mode = activity?.startSupportActionMode(actionMode)
    }

    fun clearActionMode() {
        if (mode != null) {
            mode!!.finish()
        }
    }

    override fun refreshHolder(holder: SelectableHolder?) {
        if (holder == null || holder !is ConversationViewHolder || !isSelectable ||
                Settings.baseTheme !== BaseTheme.BLACK) {
            super.refreshHolder(holder)
            return
        }

        holder.isSelectable = mIsSelectable

        val isActivated = mSelections.get(holder.adapterPosition)
        val states = if (isActivated) {
            ColorStateList.valueOf(activity!!.resources.getColor(R.color.actionModeBackground))
        } else {
            ColorStateList.valueOf(Color.BLACK)
        }

        if (holder.itemView != null) {
            holder.itemView.backgroundTintList = states
        } else {
            Log.v("ConversationMultiSelect", "null item view")
        }
    }

    override fun tapSelection(holder: SelectableHolder): Boolean {
        val result = super.tapSelection(holder)

        if (mode != null && Settings.baseTheme !== BaseTheme.BLACK) {
            mode!!.invalidate()
        }

        return result
    }

    companion object {
        fun changeMenuItemColor(item: MenuItem) {
            item.icon?.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY)
        }
    }
}
