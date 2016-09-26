/*
 * Copyright (C) 2016 Jacob Klinker
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

package xyz.klinker.messenger.adapter;

import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.io.File;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.ImageViewHolder;
import xyz.klinker.messenger.util.listener.ImageSelectedListener;

/**
 * An adapter for displaying images in a grid for the user to select to attach to a message.
 */
public class AttachImageListAdapter extends RecyclerView.Adapter<ImageViewHolder> {

    private Cursor images;
    private ImageSelectedListener callback;

    public AttachImageListAdapter(Cursor images, ImageSelectedListener callback) {
        this.images = images;
        this.callback = callback;
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attach_image, parent, false);

        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ImageViewHolder holder, int position) {
        if (position == 0) {
            holder.image.setImageResource(R.drawable.ic_photo_gallery);
        } else {
            images.moveToPosition(position - 1);
            File file = new File(images.getString(images.getColumnIndex(MediaStore.MediaColumns.DATA)));
            Uri uri = Uri.fromFile(file);

            holder.image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (callback != null) {
                        callback.onImageSelected(holder.uri);
                    }
                }
            });

            holder.uri = uri;
            Glide.with(holder.image.getContext())
                    .load(uri)
                    .centerCrop()
                    .into(holder.image);
        }
    }

    @Override
    public int getItemCount() {
        if (images == null) {
            return 0;
        } else {
            return images.getCount() + 1;
        }
    }

}
