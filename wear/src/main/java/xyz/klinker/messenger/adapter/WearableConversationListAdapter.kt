package xyz.klinker.messenger.adapter

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.bumptech.glide.Glide
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.WearableConversationViewHolder
import xyz.klinker.messenger.shared.data.SectionType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.pojo.ReorderType
import xyz.klinker.messenger.shared.shared_interfaces.IConversationListAdapter
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.TimeUtils
import java.util.*

class WearableConversationListAdapter(conversations: List<Conversation>) : SectionedRecyclerViewAdapter<WearableConversationViewHolder>(), IConversationListAdapter {

    override var conversations = mutableListOf<Conversation>()
        set(convos) {
            field = ArrayList()
            this.sectionCounts = ArrayList()

            var currentSection = 0
            var currentCount = 0

            var i = 0
            while (i < convos.size) {
                val conversation = convos[i]
                this.conversations.add(conversation)

                if (currentSection == SectionType.PINNED && conversation.pinned ||
                        currentSection == SectionType.TODAY && TimeUtils.isToday(conversation.timestamp) ||
                        currentSection == SectionType.YESTERDAY && TimeUtils.isYesterday(conversation.timestamp) ||
                        currentSection == SectionType.LAST_WEEK && TimeUtils.isLastWeek(conversation.timestamp) ||
                        currentSection == SectionType.LAST_MONTH && TimeUtils.isLastMonth(conversation.timestamp) ||
                        currentSection == SectionType.OLDER) {
                    currentCount++
                } else {
                    if (currentCount != 0) {
                        sectionCounts.add(SectionType(currentSection, currentCount))
                    }

                    currentSection++
                    currentCount = 0
                    i--
                    this.conversations.remove(conversation)
                }
                i++
            }

            sectionCounts.add(SectionType(currentSection, currentCount))
        }
    override var sectionCounts = mutableListOf<SectionType>()
        private set(value) {
            sectionCounts.clear()
            sectionCounts.addAll(value)
        }

    init {
        this.conversations.addAll(conversations)
    }

    override fun getSectionCount(): Int {
        return sectionCounts.size
    }

    override fun getItemCount(section: Int): Int {
        return sectionCounts[section].count
    }

    override fun onBindHeaderViewHolder(holder: WearableConversationViewHolder, section: Int) {
        val text = when {
            sectionCounts[section].type == SectionType.PINNED -> holder.header.context.getString(R.string.pinned)
            sectionCounts[section].type == SectionType.TODAY -> holder.header.context.getString(R.string.today)
            sectionCounts[section].type == SectionType.YESTERDAY -> holder.header.context.getString(R.string.yesterday)
            sectionCounts[section].type == SectionType.LAST_WEEK -> holder.header.context.getString(R.string.last_week)
            sectionCounts[section].type == SectionType.LAST_MONTH -> holder.header.context.getString(R.string.last_month)
            else -> holder.header.context.getString(R.string.older)
        }

        holder.header.text = text
    }

    override fun onViewRecycled(holder: WearableConversationViewHolder?) {
        super.onViewRecycled(holder)

        if (holder?.image != null) {
            try {
                Glide.with(holder.image).clear(holder.image)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }

        }
    }

    override fun onBindViewHolder(holder: WearableConversationViewHolder, section: Int, relativePosition: Int,
                                  absolutePosition: Int) {
        val conversation = this.conversations[absolutePosition]

        holder.conversation = conversation

        if (conversation.imageUri == null || conversation.imageUri!!.isEmpty()) {
            if (Settings.useGlobalThemeColor) {
                holder.image?.setImageDrawable(ColorDrawable(
                        Settings.mainColorSet.colorLight))
            } else {
                holder.image?.setImageDrawable(ColorDrawable(conversation.colors.color))
            }

            if (ContactUtils.shouldDisplayContactLetter(conversation)) {
                holder.imageLetter.text = conversation.title!!.substring(0, 1)
                if (holder.groupIcon.visibility != View.GONE) {
                    holder.groupIcon.visibility = View.GONE
                }
            } else {
                holder.imageLetter.text = null
                if (holder.groupIcon.visibility != View.VISIBLE) {
                    holder.groupIcon.visibility = View.VISIBLE
                }

                if (conversation.phoneNumbers!!.contains(",")) {
                    holder.groupIcon.setImageResource(R.drawable.ic_group)
                } else {
                    holder.groupIcon.setImageResource(R.drawable.ic_person)
                }
            }
        } else {
            holder.imageLetter.text = null
            if (holder.groupIcon.visibility != View.GONE) {
                holder.groupIcon.visibility = View.GONE
            }

            Glide.with(holder.itemView.context)
                    .load(Uri.parse(conversation.imageUri))
                    .into(holder.image)
        }

        holder.name.text = conversation.title
        if (conversation.privateNotifications || conversation.snippet == null ||
                conversation.snippet!!.contains("file://") || conversation.snippet!!.contains("content://")) {
            holder.summary.text = ""
        } else {
            holder.summary.text = conversation.snippet
        }

        // read not muted
        // not read, not muted
        // muted not read
        // read and muted
        if (conversation.read && conversation.mute && (holder.isBold || !holder.isItalic)) {
            // should be italic
            holder.setTypeface(false, true)
        } else if (conversation.mute && !conversation.read && (!holder.isItalic || !holder.isBold)) {
            // should be just italic
            holder.setTypeface(true, true)
        } else if (!conversation.mute && conversation.read && (holder.isItalic || holder.isBold)) {
            // should be not italic and not bold
            holder.setTypeface(false, false)
        } else if (!conversation.mute && !conversation.read && (holder.isItalic || !holder.isBold)) {
            // should be bold, not italic
            holder.setTypeface(true, false)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WearableConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(if (viewType == SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER)
                    R.layout.item_conversation_header
                else
                    R.layout.item_conversation,
                        parent, false)
        return WearableConversationViewHolder(view)
    }

    override fun findPositionForConversationId(id: Long): Int {
        var headersAbove = 1
        val conversationPosition = this.conversations.indices.firstOrNull { this.conversations[it].id == id } ?: -1

        if (conversationPosition == -1) {
            return -1
        }

        var totalSectionsCount = 0

        for (i in sectionCounts.indices) {
            totalSectionsCount += sectionCounts[i].count

            if (conversationPosition < totalSectionsCount) {
                break
            } else {
                headersAbove++
            }
        }

        return conversationPosition + headersAbove
    }

    override fun getCountForSection(sectionType: Int): Int {
        return sectionCounts.indices
                .firstOrNull { sectionCounts[it].type == sectionType }
                ?.let { sectionCounts[it].count }
                ?: 0
    }

    override fun removeItem(position: Int, type: ReorderType): Boolean {
        return false
    }

}
