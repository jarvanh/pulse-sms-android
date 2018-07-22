package xyz.klinker.messenger.utils.multi_select

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.bignerdranch.android.multiselector.SelectableHolder
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.message.MessageListAdapter
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder
import xyz.klinker.messenger.fragment.bottom_sheet.CopyMessageTextFragment
import xyz.klinker.messenger.fragment.bottom_sheet.MessageShareFragment
import xyz.klinker.messenger.fragment.message.MessageListFragment
import xyz.klinker.messenger.shared.data.*
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.ColorUtils
import java.util.*
import android.content.Context.INPUT_METHOD_SERVICE
import android.view.View
import android.view.inputmethod.InputMethodManager


@Suppress("DEPRECATION")
class MessageMultiSelectDelegate(private val fragment: MessageListFragment) : MultiSelector() {

    private val activity: AppCompatActivity? by lazy { fragment.activity as AppCompatActivity? }
    private var adapter: MessageListAdapter? = null

    private var mode: ActionMode? = null
    private val actionMode = object : ActionMode.Callback {
        override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            clearSelections()
            isSelectable = true

            activity?.menuInflater?.inflate(R.menu.action_mode_message_list, menu)

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val delete = menu.findItem(R.id.menu_delete_messages)
            val share = menu.findItem(R.id.menu_share_message)
            val info = menu.findItem(R.id.menu_message_details)
            val copy = menu.findItem(R.id.menu_copy_message)

            ConversationsMultiSelectDelegate.changeMenuItemColor(delete)
            ConversationsMultiSelectDelegate.changeMenuItemColor(share)
            ConversationsMultiSelectDelegate.changeMenuItemColor(info)
            ConversationsMultiSelectDelegate.changeMenuItemColor(copy)

            var checked = 0
            for (i in 0 until mSelections.size()) {
                val key = mSelections.keyAt(i)
                if (mSelections.get(key))
                    checked++
                if (checked > 1)
                    break
            }

            when {
                checked == 0 -> clearActionMode()
                checked > 1 -> {
                    delete.isVisible = true
                    share.isVisible = true
                    info.isVisible = false
                    copy.isVisible = false
                }
                else -> {
                    delete.isVisible = true
                    share.isVisible = true
                    info.isVisible = true
                    copy.isVisible = true
                }
            }

            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            clearSelections()

            // https://github.com/bignerdranch/recyclerview-multiselect/issues/9#issuecomment-140180348
            try {
                val field = this@MessageMultiSelectDelegate.javaClass.getField("mIsSelectable")
                if (field != null) {
                    if (!field.isAccessible)
                        field.isAccessible = true
                    field.set(this, false)
                }
            } catch (e: Exception) {
            }

            Handler().postDelayed({ isSelectable = false }, 250)
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity?.findViewById<View>(android.R.id.content)?.windowToken, 0)

            val handled: Boolean

            val selectedIds = ArrayList<Long>()
            var highestKey = -1
            for (i in 0 until mSelections.size()) {
                val key = mSelections.keyAt(i)
                if (highestKey == -1 || key > highestKey)
                    highestKey = key

                if (mSelections.get(key))
                    selectedIds.add(adapter!!.getItemId(key))
            }

            if (selectedIds.size == 0) {
                return false
            }

            when {
                item.itemId == R.id.menu_delete_messages -> {
                    handled = true

                    for (id in selectedIds) {
                        DataSource.deleteMessage(activity!!, id)
                    }

                    adapter!!.onMessageDeleted(activity!!, fragment.conversationId, highestKey)
                    fragment.loadMessages()
                }
                item.itemId == R.id.menu_share_message -> {
                    handled = true
                    val messages = selectedIds.map { DataSource.getMessage(activity!!, it) }
                    val conversation = DataSource.getConversation(activity!!, this@MessageMultiSelectDelegate.fragment.conversationId)

                    val fragment = MessageShareFragment()
                    fragment.setMessages(messages, conversation)
                    fragment.show(activity!!.supportFragmentManager, "")
                }
                item.itemId == R.id.menu_copy_message -> {
                    handled = true
                    val message = DataSource.getMessage(activity!!, selectedIds[0])
                    val text = MessageMultiSelectDelegate.getMessageContent(message)

                    val fragment = CopyMessageTextFragment(text!!)
                    fragment.show(activity!!.supportFragmentManager, "")
                }
                else -> {
                    handled = true
                    AlertDialog.Builder(activity!!)
                            .setMessage(DataSource.getMessageDetails(activity!!, selectedIds[0]))
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                }
            }

            mode.finish()
            return handled
        }
    }

    fun setAdapter(adapter: MessageListAdapter) {
        this.adapter = adapter
    }

    fun startActionMode() {
        mode = activity!!.startSupportActionMode(actionMode)
    }

    fun clearActionMode() {
        mode?.finish()
    }

    override fun refreshHolder(holder: SelectableHolder?) {
        if (holder == null || holder !is MessageViewHolder || !isSelectable) {
            return
        }

        val message = holder as MessageViewHolder?
        message?.isSelectable = mIsSelectable

        val isActivated = mSelections.get(message!!.adapterPosition)
        val states: ColorStateList
        val textColor: Int

        when {
            isActivated -> {
                states = ColorStateList.valueOf(activity!!.resources.getColor(R.color.actionModeBackground))
                textColor = Color.WHITE
            }
            message.color != Integer.MIN_VALUE -> {
                states = ColorStateList.valueOf(message.color)
                textColor = if (ColorUtils.isColorDark(message.color)) Color.WHITE else activity!!.resources.getColor(R.color.darkText)
            }
            else -> {
                states = ColorStateList.valueOf(activity!!.resources.getColor(R.color.drawerBackground))
                textColor = activity!!.resources.getColor(R.color.primaryText)
            }
        }

        message.messageHolder?.backgroundTintList = states
        message.message?.setTextColor(textColor)
    }

    override fun tapSelection(holder: SelectableHolder): Boolean {
        val result = super.tapSelection(holder)

        mode?.invalidate()
        return result
    }

    companion object {
        fun getMessageContent(message: Message?): String? {
            if (message == null) {
                return ""
            }

            if (MimeType.isExpandedMedia(message.mimeType)) {
                if (message.mimeType == MimeType.MEDIA_YOUTUBE_V2) {
                    val preview = YouTubePreview.build(message.data!!)
                    return if (preview != null) preview.url + "\n\n" + preview.title else ""
                } else if (message.mimeType == MimeType.MEDIA_ARTICLE) {
                    val preview = ArticlePreview.build(message.data!!)
                    return if (preview != null) preview.webUrl + "\n\n" + preview.title else ""
                }
            }

            return message.data
        }
    }
}
