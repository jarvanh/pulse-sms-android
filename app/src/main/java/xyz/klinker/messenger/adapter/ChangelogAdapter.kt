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

package xyz.klinker.messenger.adapter

import android.app.Activity
import android.content.Context
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import xyz.klinker.messenger.R

/**
 * Adapter for displaying changelog items in a list.
 */
class ChangelogAdapter(context: Context, private val items: Array<Spanned>) : ArrayAdapter<Spanned>(context, R.layout.item_changelog) {

    class ViewHolder {
        var text: TextView? = null
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var rowView = convertView

        if (rowView == null) {
            val inflater = (context as Activity).layoutInflater
            rowView = inflater.inflate(R.layout.item_changelog, parent, false)

            val viewHolder = ViewHolder()
            viewHolder.text = rowView as TextView
            rowView.tag = viewHolder
        }

        val holder = rowView.tag as ViewHolder
        holder.text?.text = items[position]

        return rowView
    }

}