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

import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import xyz.klinker.messenger.R;

/**
 * Adapter for displaying faqs items in a list. Each item should be expanded when clicked on.
 */
public class FaqsAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final String[] items;

    public static class ViewHolder {
        public TextView question;
        public TextView answer;
    }

    public FaqsAdapter(Context context, String[] items) {
        super(context, R.layout.item_faqs);
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View rowView = convertView;

        if (rowView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            rowView = inflater.inflate(R.layout.item_faqs, parent, false);

            ViewHolder viewHolder = new ViewHolder();
            viewHolder.question = (TextView) rowView.findViewById(R.id.question);
            viewHolder.answer = (TextView) rowView.findViewById(R.id.answer);

            rowView.setTag(viewHolder);
        }

        String[] item = items[position].split("<br/><br/>");

        ViewHolder holder = (ViewHolder) rowView.getTag();
        holder.question.setText(Html.fromHtml(item[0]));
        holder.answer.setText(item[1]);

        holder.answer.setVisibility(View.GONE);
        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewHolder holder = (ViewHolder) view.getTag();

                if (holder.answer.getVisibility() == View.VISIBLE) {
                    holder.answer.setVisibility(View.GONE);
                } else {
                    holder.answer.setVisibility(View.VISIBLE);
                }
            }
        });

        return rowView;
    }

}