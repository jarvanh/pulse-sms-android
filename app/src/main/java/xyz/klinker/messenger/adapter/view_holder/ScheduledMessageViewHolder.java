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

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import xyz.klinker.messenger.R;

/**
 * View holder for displaying scheduled messages content.
 */
public class ScheduledMessageViewHolder extends RecyclerView.ViewHolder {

    public TextView titleDate;
    public TextView message;
    public View messageHolder;

    public ScheduledMessageViewHolder(View itemView) {
        super(itemView);

        titleDate = (TextView) itemView.findViewById(R.id.title_date);
        message = (TextView) itemView.findViewById(R.id.message);
        messageHolder = itemView.findViewById(R.id.message_holder);
    }

}
