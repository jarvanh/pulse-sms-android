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

import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.ImageViewHolder
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.util.listener.ImageSelectedListener
import java.io.File

/**
 * An adapter for displaying images in a grid for the user to select to attach to a message.
 */
class AttachImageListAdapter(private val images: Cursor, private val callback: ImageSelectedListener?, private val colorForMediaTile: Int)
    : RecyclerView.Adapter<ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_attach_image, parent, false)

        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        if (position == 0) {
            holder.image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            holder.image.setImageResource(R.drawable.ic_photo_gallery)
            holder.image.setBackgroundColor(colorForMediaTile)

            holder.image.setOnClickListener {
                callback?.onGalleryPicker()
            }

            if (holder.playButton.visibility != View.GONE) {
                holder.playButton.visibility = View.GONE
            }

            if (holder.selectedCheckmarkLayout.visibility != View.GONE) {
                holder.selectedCheckmarkLayout.visibility = View.GONE
            }
        } else {
            try {
                images.moveToPosition(position - 1)
                val file = File(images.getString(images.getColumnIndex(MediaStore.Files.FileColumns.DATA)))
                val uri = Uri.fromFile(file)

                holder.image.setOnClickListener {
                    if (holder.uri != null) {
                        callback?.onImageSelected(holder.uri!!, holder.mimeType ?: MimeType.IMAGE_JPG)
                    }

                    if (holder.selectedCheckmarkLayout.visibility != View.VISIBLE) {
                        holder.selectedCheckmarkLayout.visibility = View.VISIBLE
                    } else {
                        holder.selectedCheckmarkLayout.visibility = View.GONE
                    }
                }

                holder.mimeType = images.getString(images.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE))
                holder.uri = uri
                holder.image.setBackgroundColor(Color.TRANSPARENT)

                Glide.with(holder.image.context).load(uri)
                        .apply(RequestOptions().centerCrop())
                        .into(holder.image)

                if (holder.mimeType != null && holder.mimeType!!.contains("video") && holder.playButton.visibility == View.GONE) {
                    holder.playButton.visibility = View.VISIBLE
                } else if (holder.playButton.visibility != View.GONE) {
                    holder.playButton.visibility = View.GONE
                }

                if (holder.selectedCheckmarkLayout.visibility != View.VISIBLE && callback!!.isCurrentlySelected(holder.uri!!, holder.mimeType!!)) {
                    holder.selectedCheckmarkLayout.visibility = View.VISIBLE
                } else if (holder.selectedCheckmarkLayout.visibility != View.GONE) {
                    holder.selectedCheckmarkLayout.visibility = View.GONE
                }
            } catch (e: Exception) {
            }
        }
    }

    override fun getItemCount() = if (images.isClosed) 0 else images.count + 1
}
