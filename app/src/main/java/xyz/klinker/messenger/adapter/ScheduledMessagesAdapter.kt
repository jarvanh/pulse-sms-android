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

package xyz.klinker.messenger.adapter

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.ScheduledMessageViewHolder
import xyz.klinker.messenger.shared.data.model.ScheduledMessage
import xyz.klinker.messenger.shared.util.listener.ScheduledMessageClickListener
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying scheduled messages in a recyclerview.
 */
class ScheduledMessagesAdapter(private val scheduledMessages: List<ScheduledMessage>, private val listener: ScheduledMessageClickListener?)
    : RecyclerView.Adapter<ScheduledMessageViewHolder>() {

    private val formatter: DateFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduledMessageViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_scheduled_message, parent, false)
        return ScheduledMessageViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ScheduledMessageViewHolder, position: Int) {
        val message = getItem(position)

        if (message.title == null || message.title!!.isEmpty()) {
            holder.titleDate.text = message.to + " - " + formatter.format(Date(message.timestamp))
        } else {
            holder.titleDate.text = message.title + " - " + formatter.format(Date(message.timestamp))
        }

        holder.message.text = message.data
        holder.messageHolder.setOnClickListener { listener?.onClick(message) }
        holder.itemView.setOnClickListener { listener?.onClick(message) }

        holder.messageHolder.setOnLongClickListener {
            listener?.onClick(message)
            true
        }
        holder.itemView.setOnLongClickListener {
            listener?.onClick(message)
            true
        }
    }

    override fun getItemCount() = scheduledMessages.size
    private fun getItem(position: Int) = scheduledMessages[position]
}
