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
 * Adapter for displaying open source projects used in a list.
 */
public class OpenSourceAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final String[] items;

    public static class ViewHolder {
        public TextView title;
        public TextView license;
    }

    public OpenSourceAdapter(Context context, String[] items) {
        super(context, R.layout.item_open_source);
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
            rowView = inflater.inflate(R.layout.item_open_source, parent, false);

            ViewHolder viewHolder = new ViewHolder();
            viewHolder.title = (TextView) rowView.findViewById(R.id.title);
            viewHolder.license = (TextView) rowView.findViewById(R.id.license);

            rowView.setTag(viewHolder);
        }

        String[] item = items[position].split("<br/><br/>");

        ViewHolder holder = (ViewHolder) rowView.getTag();
        holder.title.setText(Html.fromHtml(item[0]));
        holder.license.setText(item[1]);

        return rowView;
    }

}