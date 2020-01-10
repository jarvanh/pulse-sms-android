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

package xyz.klinker.messenger.adapter.view_holder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import xyz.klinker.messenger.R

/**
 * View holder for displaying scheduled messages content.
 */
class ScheduledMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val titleDate: TextView = itemView.findViewById<View>(R.id.title_date) as TextView
    val message: TextView = itemView.findViewById<View>(R.id.message) as TextView
    val messageHolder: View = itemView.findViewById(R.id.message_holder)
    val image: ImageView = itemView.findViewById(R.id.image) as ImageView

    init {
        image.clipToOutline = true
    }

}
