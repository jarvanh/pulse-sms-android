package xyz.klinker.messenger.adapter.message

import android.content.res.ColorStateList
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Contact
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.ColorUtils

@Suppress("DEPRECATION")
class MessageColorHelper(private val adapter: MessageListAdapter) {

    private val fromColorMapper = mutableMapOf<String, Contact>()
    private val fromColorMapperByName = mutableMapOf<String, Contact>()

    fun setMappers(from: Map<String, Contact>, fromByName: Map<String, Contact>) {
        fromColorMapper.clear()
        fromColorMapper.putAll(from)

        fromColorMapperByName.clear()
        fromColorMapperByName.putAll(fromByName)
    }

    fun getColor(holder: MessageViewHolder, message: Message): Int {
        if (Settings.useGlobalThemeColor) {
            return Integer.MIN_VALUE
        }

        if (message.type == Message.TYPE_RECEIVED && fromColorMapper.size > 1) {
            when {
                fromColorMapper.containsKey(message.from) -> {
                    val color = fromColorMapper[message.from]!!.colors.color
                    holder.messageHolder?.backgroundTintList = ColorStateList.valueOf(color)
                    holder.color = color

                    if (!ColorUtils.isColorDark(color)) {
                        holder.message?.setTextColor(holder.itemView.context.resources.getColor(R.color.darkText))
                    } else {
                        holder.message?.setTextColor(holder.itemView.context.resources.getColor(R.color.lightText))
                    }

                    return color
                }
                fromColorMapperByName.containsKey(message.from) -> {
                    val color = fromColorMapperByName[message.from]!!.colors.color
                    holder.messageHolder?.backgroundTintList = ColorStateList.valueOf(color)
                    holder.color = color

                    if (!ColorUtils.isColorDark(color)) {
                        holder.message?.setTextColor(holder.itemView.context.resources.getColor(R.color.darkText))
                    } else {
                        holder.message?.setTextColor(holder.itemView.context.resources.getColor(R.color.lightText))
                    }

                    return color
                }
                else -> {
                    val contact = Contact()
                    contact.name = message.from
                    contact.phoneNumber = message.from
                    contact.colors = ColorUtils.getRandomMaterialColor(holder.itemView.context)

                    fromColorMapper.put(message.from!!, contact)

                    // then write it to the database for later
                    Thread {
                        val context = holder.itemView.context
                        val source = DataSource

                        if (contact.phoneNumber != null) {
                            val originalLength = contact.phoneNumber!!.length
                            val newLength = contact.phoneNumber!!.replace("[0-9]".toRegex(), "").length
                            if (originalLength == newLength) {
                                // all letters, so we should use the contact name to find the phone number
                                val contacts = source.getContactsByNames(context, contact.name)
                                if (contacts.size > 0) {
                                    contact.phoneNumber = contacts[0].phoneNumber
                                }
                            }

                            source.insertContact(context, contact)
                        }
                    }.start()

                    return contact.colors.color
                }
            }
        }

        return Integer.MIN_VALUE
    }
}