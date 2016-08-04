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
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.ScheduledMessageViewHolder;
import xyz.klinker.messenger.data.model.ScheduledMessage;
import xyz.klinker.messenger.util.listener.ScheduledMessageClickListener;

/**
 * Adapter for displaying scheduled messages in a recyclerview.
 */
public class ScheduledMessagesAdapter extends RecyclerView.Adapter<ScheduledMessageViewHolder> {

    private Cursor cursor;
    private DateFormat formatter;
    private ScheduledMessageClickListener listener;

    public ScheduledMessagesAdapter(Cursor cursor, ScheduledMessageClickListener listener) {
        this.cursor = cursor;
        this.formatter = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT,
                SimpleDateFormat.SHORT);
        this.listener = listener;
    }

    @Override
    public ScheduledMessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.scheduled_message_item, parent, false);
        return new ScheduledMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ScheduledMessageViewHolder holder, int position) {
        cursor.moveToPosition(position);

        final ScheduledMessage message = new ScheduledMessage();
        message.fillFromCursor(cursor);

        holder.title.setText(message.title);
        holder.message.setText(message.data);
        holder.date.setText(formatter.format(new Date(message.timestamp)));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onClick(message);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (cursor == null) {
            return 0;
        } else {
            return cursor.getCount();
        }
    }

}
