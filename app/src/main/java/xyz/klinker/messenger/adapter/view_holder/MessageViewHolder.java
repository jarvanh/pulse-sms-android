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

package xyz.klinker.messenger.adapter.view_holder;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.ImageViewerActivity;

/**
 * View holder for working with a message.
 */
public class MessageViewHolder extends RecyclerView.ViewHolder {

    public TextView message;
    public TextView timestamp;
    public TextView contact;
    public ImageView image;
    public View messageHolder;
    public long messageId;

    public MessageViewHolder(final View itemView, int color, final long conversationId) {
        super(itemView);

        message = (TextView) itemView.findViewById(R.id.message);
        timestamp = (TextView) itemView.findViewById(R.id.timestamp);
        contact = (TextView) itemView.findViewById(R.id.contact);
        image = (ImageView) itemView.findViewById(R.id.image);
        messageHolder = itemView.findViewById(R.id.message_holder);

        if (color != -1 && messageHolder != null) {
            messageHolder.setBackgroundTintList(ColorStateList.valueOf(color));
        }

        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(itemView.getContext(), ImageViewerActivity.class);
                intent.putExtra(ImageViewerActivity.EXTRA_CONVERSATION_ID, conversationId);
                intent.putExtra(ImageViewerActivity.EXTRA_MESSAGE_ID, messageId);
                itemView.getContext().startActivity(intent);
            }
        });
    }

}
