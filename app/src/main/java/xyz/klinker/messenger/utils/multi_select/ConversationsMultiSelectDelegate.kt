package xyz.klinker.messenger.utils.multi_select

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.TooltipCompat
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
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
import xyz.klinker.messenger.shared.util.DensityUtil
import java.util.*

@Suppress("DEPRECATION")
class ConversationsMultiSelectDelegate(private val fragment: ConversationListFragment) : MultiSelector() {

    private val activity: AppCompatActivity by lazy { fragment.activity as AppCompatActivity }
    private var adapter: ConversationListAdapter? = null

    private var mode: ActionMode? = null
    private val actionMode = object : ModalMultiSelectorCallback(this) {
        override fun onCreateActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
            super.onCreateActionMode(actionMode, menu)

            if (fragment is ArchivedConversationListFragment) {
                activity.menuInflater.inflate(R.menu.action_mode_archive_list, menu)

                val unarchive = menu!!.findItem(R.id.menu_archive_conversation)
                fixMenuItemLongClickCrash(actionMode, unarchive, R.drawable.ic_unarchive, R.string.menu_move_to_inbox)
            } else {
                activity.menuInflater.inflate(R.menu.action_mode_conversation_list, menu)

                val archive = menu!!.findItem(R.id.menu_archive_conversation)
                val pin = menu.findItem(R.id.menu_pin_conversation)
                val mute = menu.findItem(R.id.menu_mute_conversation)

                fixMenuItemLongClickCrash(actionMode, archive, R.drawable.ic_archive, R.string.menu_archive_conversation)
                fixMenuItemLongClickCrash(actionMode, pin, R.drawable.ic_pin, R.string.pin_conversation)
                fixMenuItemLongClickCrash(actionMode, mute, R.drawable.ic_mute, R.string.mute_conversation)
            }

            val delete = menu.findItem(R.id.menu_delete_conversation)
            fixMenuItemLongClickCrash(actionMode, delete, R.drawable.ic_delete, R.string.menu_delete_conversation)

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
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
                        source.updateConversationSettings(activity, conversation)
                    }

                    fragment.recyclerManager.loadConversations()
                }
                R.id.menu_pin_conversation -> {
                    handled = true

                    for (conversation in selectedConversations) {
                        conversation.pinned = !conversation.pinned
                        source.updateConversationSettings(activity, conversation)
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
        mode = activity.startSupportActionMode(actionMode)
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
            ColorStateList.valueOf(activity.resources.getColor(R.color.actionModeBackground))
        } else {
            ColorStateList.valueOf(Color.BLACK)
        }

        if (holder.itemView != null) {
            holder.itemView.backgroundTintList = states
        }
    }

    override fun tapSelection(holder: SelectableHolder): Boolean {
        val result = super.tapSelection(holder)

        if (mode != null && Settings.baseTheme !== BaseTheme.BLACK) {
            mode!!.invalidate()
        }

        return result
    }


    private fun fixMenuItemLongClickCrash(mode: ActionMode?, item: MenuItem, icon: Int, text: Int) {
        try {
            val image = ImageView(activity)
            image.setImageResource(icon)
            image.setPaddingRelative(0, 0, DensityUtil.toDp(activity, 24), 0)
            image.imageTintList = ColorStateList.valueOf(Color.WHITE)
            image.alpha = 1.0f

            item.actionView = image
            TooltipCompat.setTooltipText(item.actionView, fragment.getString(text))

            image.setOnClickListener { actionMode.onActionItemClicked(mode!!, item) }

            image.setOnLongClickListener {
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
