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

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.BlacklistViewHolder;
import xyz.klinker.messenger.data.model.Blacklist;
import xyz.klinker.messenger.util.PhoneNumberUtils;
import xyz.klinker.messenger.util.listener.BlacklistClickedListener;

/**
 * A simple adapter that displays a formatted phone number in a list.
 */
public class BlacklistAdapter extends RecyclerView.Adapter<BlacklistViewHolder> {

    private Cursor cursor;
    private BlacklistClickedListener listener;

    public BlacklistAdapter(Cursor cursor, BlacklistClickedListener listener) {
        this.cursor = cursor;
        this.listener = listener;
    }

    @Override
    public BlacklistViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_blacklist, parent, false);
        final BlacklistViewHolder holder = new BlacklistViewHolder(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onClick(holder.getAdapterPosition());
                }
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(BlacklistViewHolder holder, int position) {
        cursor.moveToPosition(position);
        String number = cursor.getString(cursor.getColumnIndex(Blacklist.COLUMN_PHONE_NUMBER));

        holder.text.setText(PhoneNumberUtils.format(number));
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
