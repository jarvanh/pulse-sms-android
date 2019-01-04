/*
 * Copyright (C) 2017 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.adapter.view_holder

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import de.hdodenhof.circleimageview.CircleImageView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.pojo.BaseTheme
import xyz.klinker.messenger.shared.util.AnimationUtils
import xyz.klinker.messenger.shared.util.DensityUtil
import xyz.klinker.messenger.shared.util.listener.ContactClickedListener
import xyz.klinker.messenger.utils.listener.ConversationExpandedListener

@Suppress("DEPRECATION")
/**
 * View holder for recycling inflated conversations.
 */
class ConversationViewHolder(itemView: View, private val expandedListener: ConversationExpandedListener?, adapter: ConversationListAdapter?)
    : SwappingHolder(itemView, if (adapter == null || adapter.multiSelector == null) MultiSelector() else adapter.multiSelector) {

    private val conversationImageHolder: View? by lazy { itemView.findViewById<View>(R.id.image_holder) }
    private val headerBackground: View? by lazy { itemView.findViewById<View>(R.id.header_background) }

    val unreadIndicator: View? by lazy { itemView.findViewById<View>(R.id.unread_indicator) }
    val header: TextView? by lazy { itemView.findViewById<View>(R.id.header) as TextView? }
    val headerDone: ImageButton? by lazy { itemView.findViewById<View>(R.id.section_done) as ImageButton? }
    val headerCardForTextOnline: View? by lazy { itemView.findViewById<View>(R.id.header_card) }
    val image: CircleImageView? by lazy { itemView.findViewById<View>(R.id.image) as CircleImageView? }
    val name: TextView? by lazy { itemView.findViewById<View>(R.id.name) as TextView? }
    val summary: TextView? by lazy { itemView.findViewById<View>(R.id.summary) as TextView? }
    val imageLetter: TextView? by lazy { itemView.findViewById<View>(R.id.image_letter) as TextView? }
    val groupIcon: ImageView? by lazy { itemView.findViewById<View>(R.id.group_icon) as ImageView? }
    val checkBox: CheckBox? by lazy { itemView.findViewById<View>(R.id.checkbox) as CheckBox? }

    var conversation: Conversation? = null
    var absolutePosition: Int = 0

    private var expanded = false
    private var contactClickedListener: ContactClickedListener? = null

    private val isBold: Boolean
        get() = name!!.typeface != null && name!!.typeface.isBold

    private val isItalic: Boolean
        get() = name!!.typeface != null && name!!.typeface.style == Typeface.ITALIC

    init {
        if (header == null) {
            selectionModeBackgroundDrawable = itemView.resources.getDrawable(R.drawable.conversation_list_item_selectable_background)
        }

        itemView.setOnClickListener {
            if (header == null && (adapter?.multiSelector != null &&
                            !adapter.multiSelector.tapSelection(this@ConversationViewHolder) || adapter == null)) {
                if (conversation == null) {
                    return@setOnClickListener
                }

                if (header == null) {
                    try {
                        adapter!!.conversations[absolutePosition].read = true
                    } catch (e: Exception) {
                    }

                    setTypeface(false, isItalic)
                }

                if (expandedListener != null) {
                    changeExpandedState()
                }

                contactClickedListener?.onClicked(conversation!!)
                checkBox?.isChecked = checkBox?.isChecked != true
            }
        }

        itemView.setOnLongClickListener { startMultiSelect(adapter) }
        image?.setOnClickListener {
            val consumedClick = startMultiSelect(adapter)
            if (!consumedClick) {
                itemView.performClick()
            }
        }

        header?.textSize = Settings.smallFont.toFloat() + 1
        name?.textSize = Settings.largeFont.toFloat()
        summary?.textSize = Settings.mediumFont.toFloat()

        if (Settings.smallFont == 10 && conversationImageHolder != null) {
            // user selected small font from the settings
            val fourtyDp = DensityUtil.toDp(itemView.context, 40)
            conversationImageHolder!!.layoutParams.height = fourtyDp
            conversationImageHolder!!.layoutParams.width = fourtyDp
            conversationImageHolder!!.invalidate()

            itemView.layoutParams.height = DensityUtil.toDp(itemView.context, 66)
            itemView.invalidate()
        }

        if (Settings.baseTheme === BaseTheme.BLACK && headerBackground != null) {
            headerBackground!!.setBackgroundColor(Color.BLACK)
        } else if (Settings.baseTheme === BaseTheme.BLACK) {
            itemView.setBackgroundColor(Color.BLACK)
        }
    }

    fun setTypeface(bold: Boolean, italic: Boolean) {
        if (bold) {
            name?.setTypeface(Typeface.DEFAULT_BOLD, if (italic) Typeface.ITALIC else Typeface.NORMAL)
            summary?.setTypeface(Typeface.DEFAULT_BOLD, if (italic) Typeface.ITALIC else Typeface.NORMAL)
            unreadIndicator?.visibility = View.VISIBLE

            var color = Settings.mainColorSet.color
            if (color == Color.WHITE) {
                color = itemView.context.resources.getColor(R.color.lightToolbarTextColor)
            } else if (color == Color.BLACK && Settings.baseTheme === BaseTheme.BLACK) {
                color = itemView.context.resources.getColor(android.R.color.white)
            }

            (unreadIndicator as CircleImageView).setImageDrawable(ColorDrawable(color))
        } else {
            name?.setTypeface(Typeface.DEFAULT, if (italic) Typeface.ITALIC else Typeface.NORMAL)
            summary?.setTypeface(Typeface.DEFAULT, if (italic) Typeface.ITALIC else Typeface.NORMAL)
            unreadIndicator?.visibility = View.GONE
        }
    }

    private fun changeExpandedState() {
        if (header != null) {
            return
        }

        if (expanded) {
            collapseConversation()
        } else {
            expandConversation()
        }
    }

    private fun expandConversation() {
        if (expandedListener?.onConversationExpanded(this) == true) {
            expanded = true
            AnimationUtils.expandConversationListItem(itemView)
        }
    }

    private fun collapseConversation() {
        expanded = false
        expandedListener?.onConversationContracted(this)
        AnimationUtils.contractConversationListItem(itemView)
    }

    private fun startMultiSelect(adapter: ConversationListAdapter?): Boolean {
        if (header != null) {
            return true
        }

        val multiSelect = adapter?.multiSelector
        if (multiSelect != null && !multiSelect.isSelectable) {
            multiSelect.startActionMode()
            multiSelect.isSelectable = true
            multiSelect.setSelected(this@ConversationViewHolder, true)
            return true
        }

        return false
    }

    fun setContactClickedListener(listener: ContactClickedListener?) {
        this.contactClickedListener = listener
    }
}
