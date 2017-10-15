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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout

import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.view.ColorPreviewButton

/**
 * Adapter for choosing a color to apply to an entire conversation.
 */
class ColorPickerAdapter(context: Context, private val colors: List<ColorSet>,
                         private val itemClickedListener: View.OnClickListener)
    : ArrayAdapter<ColorSet>(context, android.R.layout.simple_list_item_1, colors) {

    val frameLayout: FrameLayout
        get() = FrameLayout(context)

    val colorPreviewButton: ColorPreviewButton
        get() = ColorPreviewButton(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val color = colors[position]
        val frame = frameLayout
        val button = colorPreviewButton
        button.setInnerColor(color.color)
        button.setOuterColor(color.colorAccent)
        frame.addView(button)

        button.setOnClickListener {
            button.tag = position
            itemClickedListener.onClick(button)
        }

        return frame
    }
}
