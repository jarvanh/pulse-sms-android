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

import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView

import xyz.klinker.messenger.R

/**
 * View holder for keeping a single image.
 */
class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val image: ImageView = itemView.findViewById<View>(R.id.image) as ImageView
    val selectedCheckmarkLayout: ImageView = itemView.findViewById<View>(R.id.selected_checkmark_layout) as ImageView
    val playButton: ImageView = itemView.findViewById<View>(R.id.play_button) as ImageView
    var uri: Uri? = null
    var mimeType: String? = null

}
