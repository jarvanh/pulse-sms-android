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
import com.bumptech.glide.request.RequestOptions;

import java.io.File;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.ImageViewHolder;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.util.listener.ImageSelectedListener;

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
            holder.getImage().setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            holder.getImage().setImageResource(R.drawable.ic_photo_gallery);
            holder.getImage().setBackgroundColor(colorForMediaTile);

            holder.getImage().setOnClickListener(view -> {
                if (callback != null) {
                    callback.onGalleryPicker();
                }
            });

            if (holder.getPlayButton().getVisibility() != View.GONE) {
                holder.getPlayButton().setVisibility(View.GONE);
            }

            if (holder.getSelectedCheckmarkLayout().getVisibility() != View.GONE) {
                holder.getSelectedCheckmarkLayout().setVisibility(View.GONE);
            }
        } else {
            images.moveToPosition(position - 1);
            File file = new File(images.getString(images.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
            Uri uri = Uri.fromFile(file);

            holder.getImage().setOnClickListener(view -> {
                if (callback != null) {
                    callback.onImageSelected(holder.getUri(), holder.getMimeType());
                }

                if (holder.getSelectedCheckmarkLayout().getVisibility() != View.VISIBLE) {
                    holder.getSelectedCheckmarkLayout().setVisibility(View.VISIBLE);
                } else {
                    holder.getSelectedCheckmarkLayout().setVisibility(View.GONE);
                }
            });

            holder.setMimeType(images.getString(images.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)));
            holder.setUri(uri);
            holder.getImage().setBackgroundColor(Color.TRANSPARENT);
            Glide.with(holder.getImage().getContext())
                    .load(uri)
                    .apply(new RequestOptions().centerCrop())
                    .into(holder.getImage());

            if (holder.getMimeType() != null && holder.getMimeType().contains("video") && holder.getPlayButton().getVisibility() == View.GONE) {
                holder.getPlayButton().setVisibility(View.VISIBLE);
            } else if (holder.getPlayButton().getVisibility() != View.GONE) {
                holder.getPlayButton().setVisibility(View.GONE);
            }

            if (holder.getSelectedCheckmarkLayout().getVisibility() != View.VISIBLE &&
                    callback.isCurrentlySelected(holder.getUri(), holder.getMimeType())) {
                holder.getSelectedCheckmarkLayout().setVisibility(View.VISIBLE);
            } else if (holder.getSelectedCheckmarkLayout().getVisibility() != View.GONE) {
                holder.getSelectedCheckmarkLayout().setVisibility(View.GONE);
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
