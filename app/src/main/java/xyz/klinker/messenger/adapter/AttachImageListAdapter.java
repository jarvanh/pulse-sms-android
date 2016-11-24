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
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.ImageViewHolder;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.util.listener.ImageSelectedListener;

/**
 * An adapter for displaying images in a grid for the user to select to attach to a message.
 */
public class AttachImageListAdapter extends RecyclerView.Adapter<ImageViewHolder> {

    private Cursor images;
    private ImageSelectedListener callback;
    private int colorForMediaTile;

    public AttachImageListAdapter(Cursor images, ImageSelectedListener callback, int colorForMediaTile) {
        this.images = images;
        this.callback = callback;
        this.colorForMediaTile = colorForMediaTile;
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
            holder.image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            holder.image.setImageResource(R.drawable.ic_photo_gallery);
            holder.image.setBackgroundColor(colorForMediaTile);

            holder.image.setOnClickListener(view -> {
                if (callback != null) {
                    callback.onGalleryPicker();
                }
            });

            if (holder.playButton.getVisibility() != View.GONE) {
                holder.playButton.setVisibility(View.GONE);
            }

            if (holder.selectedCheckmarkLayout.getVisibility() != View.GONE) {
                holder.selectedCheckmarkLayout.setVisibility(View.GONE);
            }
        } else {
            images.moveToPosition(position - 1);
            File file = new File(images.getString(images.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
            Uri uri = Uri.fromFile(file);

            holder.image.setOnClickListener(view -> {
                if (callback != null) {
                    callback.onImageSelected(holder.uri, holder.mimeType);
                }

                if (holder.selectedCheckmarkLayout.getVisibility() != View.VISIBLE) {
                    holder.selectedCheckmarkLayout.setVisibility(View.VISIBLE);
                } else {
                    holder.selectedCheckmarkLayout.setVisibility(View.GONE);
                }
            });

            holder.mimeType = images.getString(images.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE));
            holder.uri = uri;
            holder.image.setBackgroundColor(Color.TRANSPARENT);
            Glide.with(holder.image.getContext())
                    .load(uri)
                    .centerCrop()
                    .into(holder.image);

            if (holder.mimeType != null && holder.mimeType.contains("video") && holder.playButton.getVisibility() == View.GONE) {
                holder.playButton.setVisibility(View.VISIBLE);
            } else if (holder.playButton.getVisibility() != View.GONE) {
                holder.playButton.setVisibility(View.GONE);
            }

            if (holder.selectedCheckmarkLayout.getVisibility() != View.VISIBLE &&
                    callback.isCurrentlySelected(holder.uri, holder.mimeType)) {
                holder.selectedCheckmarkLayout.setVisibility(View.VISIBLE);
            } else if (holder.selectedCheckmarkLayout.getVisibility() != View.GONE) {
                holder.selectedCheckmarkLayout.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (images == null || images.isClosed()) {
            return 0;
        } else {
            return images.getCount() + 1;
        }
    }

}
