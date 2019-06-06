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
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import xyz.klinker.messenger.R

/**
 * Adapter for displaying open source projects used in a list.
 */
@Suppress("DEPRECATION")
class OpenSourceAdapter(context: Context, private val items: Array<String>) : ArrayAdapter<String>(context, R.layout.item_open_source) {

    class ViewHolder {
        var title: TextView? = null
        var license: TextView? = null
    }

    override fun getCount() = items.size

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var rowView = convertView

        if (rowView == null) {
            val inflater = (context as Activity).layoutInflater
            rowView = inflater.inflate(R.layout.item_open_source, parent, false)

            val viewHolder = ViewHolder()
            viewHolder.title = rowView!!.findViewById<View>(R.id.title) as TextView
            viewHolder.license = rowView.findViewById<View>(R.id.license) as TextView

            rowView.tag = viewHolder
        }

        val item = items[position].split("<br/><br/>".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val holder = rowView.tag as ViewHolder
        holder.title?.text = Html.fromHtml(item[0])
        holder.license?.text = item[1]

        return rowView
    }

}