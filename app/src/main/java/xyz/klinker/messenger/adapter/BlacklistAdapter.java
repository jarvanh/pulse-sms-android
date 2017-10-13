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
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.BlacklistViewHolder;
import xyz.klinker.messenger.shared.data.model.Blacklist;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.listener.BlacklistClickedListener;

/**
 * A simple adapter that displays a formatted phone number in a list.
 */
public class BlacklistAdapter extends RecyclerView.Adapter<BlacklistViewHolder> {

    private List<Blacklist> blacklists;
    private BlacklistClickedListener listener;

    public BlacklistAdapter(List<Blacklist> blacklists, BlacklistClickedListener listener) {
        this.listener = listener;
        this.blacklists = blacklists;
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
        String number = getItem(position).getPhoneNumber();
        holder.getText().setText(PhoneNumberUtils.format(number));
    }

    @Override
    public int getItemCount() {
        if (blacklists == null) {
            return 0;
        } else {
            return blacklists.size();
        }
    }

    public Blacklist getItem(int postion) {
        return blacklists.get(postion);
    }

}
