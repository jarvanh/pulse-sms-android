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
import android.support.design.R.id.message
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.ScheduledMessageViewHolder
import xyz.klinker.messenger.adapter.view_holder.TemplateViewHolder
import xyz.klinker.messenger.shared.data.model.ScheduledMessage
import xyz.klinker.messenger.shared.data.model.Template
import xyz.klinker.messenger.shared.util.listener.ScheduledMessageClickListener
import xyz.klinker.messenger.shared.util.listener.TemplateClickListener
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying scheduled messages in a recyclerview.
 */
class TemplateAdapter(private val templates: List<Template>, private val listener: TemplateClickListener)
    : RecyclerView.Adapter<TemplateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_template, parent, false)
        return TemplateViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        val template = getItem(position)
        holder.text.text = template.text

        holder.text.setOnClickListener { listener.onClick(template) }
        holder.itemView.setOnClickListener { listener.onClick(template) }

        holder.text.setOnLongClickListener {
            listener.onLongClick(template)
            true
        }
        holder.itemView.setOnLongClickListener {
            listener.onLongClick(template)
            true
        }
    }

    override fun getItemCount() = templates.size
    private fun getItem(position: Int) = templates[position]
}
