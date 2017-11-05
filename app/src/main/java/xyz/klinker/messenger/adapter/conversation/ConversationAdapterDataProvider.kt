package xyz.klinker.messenger.adapter.conversation

import android.app.Activity
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper
import xyz.klinker.messenger.shared.data.SectionType
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.pojo.ReorderType
import xyz.klinker.messenger.shared.util.TimeUtils

class ConversationAdapterDataProvider(private val adapter: ConversationListAdapter, private val activity: Activity) {

    val conversations = mutableListOf<Conversation>()
    val sectionCounts = mutableListOf<SectionType>()

    fun generateSections(newConversations: List<Conversation>) {
        conversations.clear()
        sectionCounts.clear()

        if (adapter.showHeaderAboutTextingOnline()) {
            sectionCounts.add(SectionType(SectionType.CARD_ABOUT_ONLINE, 0))
            AnalyticsHelper.convoListCardShown(activity)
        }

        var currentSection = 0
        var currentCount = 0

        var i = 0
        while (i < newConversations.size) {
            val conversation = newConversations[i]
            conversations.add(conversation)

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
                conversations.remove(conversation)
            }

            i++
        }

        sectionCounts.add(SectionType(currentSection, currentCount))
    }

    fun removeItem(position: Int, reorderType: ReorderType): Boolean {
        var position = position
        if (position == -1) {
            return false
        }

        // The logic here can get a little tricky because we are removing items from the adapter
        // but need to account for the headers taking up a position as well. On top of that, if all
        // the items in a section are gone, then there shouldn't be a header for that section.

        var removedHeader = false

        val originalPosition = position
        var headersAbove = 1
        var currentTotal = 0

        try {
            for (type in sectionCounts) {
                currentTotal += type.count + 1 // +1 for the header above the section

                if (position < currentTotal) {
                    position -= headersAbove

                    val section = sectionCounts[headersAbove - 1]
                    section.count = section.count - 1

                    sectionCounts[headersAbove - 1] = section
                    val deletedConversation = conversations.removeAt(position)

                    if (section.count == 0) {
                        sectionCounts.removeAt(headersAbove - 1)
                        adapter.notifyItemRangeRemoved(originalPosition - 1, 2)
                        removedHeader = true
                    } else {
                        adapter.notifyItemRemoved(originalPosition)
                    }

                    if (reorderType === ReorderType.DELETE) {
                        adapter.swipeToDeleteListener.onSwipeToDelete(deletedConversation)
                    } else if (reorderType === ReorderType.ARCHIVE) {
                        adapter.swipeToDeleteListener.onSwipeToArchive(deletedConversation)
                    }

                    break
                } else {
                    headersAbove++
                }
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
        }

        return removedHeader
    }

    fun findPositionForConversationId(conversationId: Long): Int {
        var headersAbove = 1
        val conversationPosition = (0 until conversations.size).firstOrNull { conversations[it].id == conversationId } ?: -1

        if (conversationPosition == -1) {
            return -1
        }

        var totalSectionsCount = 0

        for (i in 0 until sectionCounts.size) {
            totalSectionsCount += sectionCounts[i].count

            if (conversationPosition < totalSectionsCount) {
                break
            } else {
                headersAbove++
            }
        }

        return conversationPosition + headersAbove
    }

    fun findConversationForPosition(position: Int): Conversation {
        var headersAbove = 0
        var totalSectionsCount = 0

        for (i in 0 until sectionCounts.size) {
            totalSectionsCount += sectionCounts[i].count

            if (position <= totalSectionsCount + headersAbove) {
                headersAbove++
                break
            } else {
                if (sectionCounts[i].count != 0) {
                    // only add the header if it has more than 0 items, otherwise it isn't shown
                    headersAbove++
                }
            }
        }

        val convoPosition = position - headersAbove

        return if (convoPosition == conversations.size) {
            conversations[convoPosition - 1]
        } else {
            conversations[convoPosition]
        }
    }

    fun getCountForSection(sectionType: Int): Int {
        return (0 until sectionCounts.size)
                .firstOrNull { sectionCounts[it].type == sectionType }
                ?.let { sectionCounts[it].count }
                ?: 0
    }
}