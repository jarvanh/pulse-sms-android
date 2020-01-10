/*
 * Copyright (C) 2020 Luke Klinker
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

package xyz.klinker.messenger.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

import com.bumptech.glide.Glide

import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.PhoneNumberUtils
import xyz.klinker.messenger.shared.util.listener.ContactClickedListener

/**
 * Adapter for displaying a list of contacts. Each contact should be loaded into a conversation
 * object for easy use, the only fields that need filled are title, phoneNumbers and imageUri.
 */
@Suppress("DEPRECATION")
open class ContactAdapter(val conversations: List<Conversation>, private val listener: ContactClickedListener?)
    : RecyclerView.Adapter<ConversationViewHolder>() {

    private var lightToolbarTextColor = Integer.MIN_VALUE

    open val layoutId: Int
        @LayoutRes get() = R.layout.contact_list_item

    override fun getItemCount() = conversations.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        val holder = ConversationViewHolder(view, null, null)
        holder.setContactClickedListener(listener)

        if (lightToolbarTextColor == Integer.MIN_VALUE) {
            this.lightToolbarTextColor = parent.context.resources.getColor(R.color.lightToolbarTextColor)
        }

        return holder
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = conversations[position]

        holder.conversation = conversation

        if (conversation.imageUri == null || conversation.imageUri!!.isEmpty()) {
            setIconColor(conversation, holder)

            if (ContactUtils.shouldDisplayContactLetter(conversation)) {
                showContactLetter(conversation, holder)
            } else {
                showContactPlaceholderImage(conversation, holder)
            }
        } else {
            showContactImage(conversation, holder)
        }

        holder.name?.text = conversation.title
        holder.summary?.text = PhoneNumberUtils.format(conversation.phoneNumbers)
    }

    private fun setIconColor(conversation: Conversation, holder: ConversationViewHolder) {
        if (Settings.useGlobalThemeColor) {
            if (Settings.mainColorSet.colorLight == Color.WHITE) {
                holder.image?.setImageDrawable(ColorDrawable(Settings.mainColorSet.colorDark))
            } else {
                holder.image?.setImageDrawable(ColorDrawable(Settings.mainColorSet.colorLight))
            }
        } else if (conversation.colors.color == Color.WHITE) {
            holder.image?.setImageDrawable(ColorDrawable(conversation.colors.colorDark))
        } else {
            holder.image?.setImageDrawable(ColorDrawable(conversation.colors.color))
        }
    }

    private fun showContactLetter(conversation: Conversation, holder: ConversationViewHolder) {
        holder.imageLetter?.text = conversation.title!!.substring(0, 1)
        if (holder.groupIcon?.visibility != View.GONE) {
            holder.groupIcon?.visibility = View.GONE
        }

        if (ColorUtils.isColorDark(if (Settings.useGlobalThemeColor) Settings.mainColorSet.color else conversation.colors.color)) {
            holder.imageLetter?.setTextColor(Color.WHITE)
        } else {
            holder.imageLetter?.setTextColor(lightToolbarTextColor)
        }
    }

    private fun showContactPlaceholderImage(conversation: Conversation, holder: ConversationViewHolder) {
        holder.imageLetter?.text = null

        if (holder.groupIcon?.visibility != View.VISIBLE) {
            holder.groupIcon?.visibility = View.VISIBLE
        }

        if (conversation.phoneNumbers!!.contains(",")) {
            holder.groupIcon?.setImageResource(R.drawable.ic_group)
        } else {
            holder.groupIcon?.setImageResource(R.drawable.ic_person)
        }

        if (ColorUtils.isColorDark(if (Settings.useGlobalThemeColor) Settings.mainColorSet.color else conversation.colors.color)) {
            holder.groupIcon?.imageTintList = ColorStateList.valueOf(Color.WHITE)
        } else {
            holder.groupIcon?.imageTintList = ColorStateList.valueOf(lightToolbarTextColor)
        }
    }

    private fun showContactImage(conversation: Conversation, holder: ConversationViewHolder) {
        if (!conversation.imageUri!!.endsWith("/photo")) {
            conversation.imageUri = conversation.imageUri!! + "/photo"
        }

        holder.imageLetter?.text = null
        if (holder.groupIcon?.visibility != View.GONE) {
            holder.groupIcon?.visibility = View.GONE
        }

        try {
            Glide.with(holder.itemView.context)
                    .load(Uri.parse(conversation.imageUri))
                    .into(holder.image!!)
        } catch (e: Exception) {
        }
    }
}
