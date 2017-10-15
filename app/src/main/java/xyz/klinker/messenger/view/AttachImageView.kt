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

package xyz.klinker.messenger.view

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.os.Handler
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.AttachImageListAdapter
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.CursorUtil
import xyz.klinker.messenger.shared.util.listener.ImageSelectedListener

/**
 * View that displays a list of images that are currently on your device and allows you to choose
 * one to attach to a message.
 */
@SuppressLint("ViewConstructor")
class AttachImageView(context: Context, private val callback: ImageSelectedListener, color: Int) : RecyclerView(context) {

    private var images: Cursor? = null

    init {
        ColorUtils.changeRecyclerOverscrollColors(this, color)
        val handler = Handler()

        Thread {
            val select = arrayOf(BaseColumns._ID, MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.Files.FileColumns.MIME_TYPE)

            val where = arrayOf("" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE, "" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

            val cr = context.contentResolver
            images = Images.Media.query(cr, MediaStore.Files.getContentUri("external"),
                    select, "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=? OR " +
                    MediaStore.Files.FileColumns.MEDIA_TYPE + "=?) AND " +
                    MediaStore.Files.FileColumns.DATA + " NOT LIKE '%http%'",
                    where, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC")

            if (images == null) {
                return@Thread
            }

            handler.post {
                layoutManager = GridLayoutManager(context, resources.getInteger(R.integer.images_column_count))
                adapter = AttachImageListAdapter(images!!, callback, color)
            }
        }.start()
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        CursorUtil.closeSilent(images)
    }

}
