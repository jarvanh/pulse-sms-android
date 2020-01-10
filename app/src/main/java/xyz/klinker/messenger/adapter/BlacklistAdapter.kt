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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.BlacklistViewHolder
import xyz.klinker.messenger.shared.data.model.Blacklist
import xyz.klinker.messenger.shared.util.PhoneNumberUtils
import xyz.klinker.messenger.shared.util.listener.BlacklistClickedListener

/**
 * A simple adapter that displays a formatted phone number in a list.
 */
class BlacklistAdapter(private val blacklists: List<Blacklist>, private val listener: BlacklistClickedListener?) : RecyclerView.Adapter<BlacklistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlacklistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_blacklist, parent, false)
        val holder = BlacklistViewHolder(view)

        view.setOnClickListener { listener?.onClick(holder.adapterPosition) }
        view.setOnLongClickListener {
            listener?.onClick(holder.adapterPosition)
            true
        }

        return holder
    }

    override fun onBindViewHolder(holder: BlacklistViewHolder, position: Int) {
        val phoneNumber = getItem(position).phoneNumber
        val phrase = getItem(position).phrase

        if (!phoneNumber.isNullOrBlank()) {
            holder.text.text = PhoneNumberUtils.format(phoneNumber)
        } else {
            holder.text.text = phrase
        }
    }

    override fun getItemCount(): Int {
        return blacklists.size
    }

    fun getItem(position: Int): Blacklist {
        return blacklists[position]
    }

}
