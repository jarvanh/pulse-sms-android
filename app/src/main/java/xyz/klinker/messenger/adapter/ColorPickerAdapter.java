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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;

import java.util.List;

import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.view.ColorPreviewButton;

/**
 * Adapter for choosing a color to apply to an entire conversation.
 */
public class ColorPickerAdapter extends ArrayAdapter<ColorSet> {

    private List<ColorSet> colors;
    private View.OnClickListener itemClickedListener;

    public ColorPickerAdapter(Context context, List<ColorSet> colors,
                              View.OnClickListener itemClickedListener) {
        super(context, android.R.layout.simple_list_item_1, colors);
        this.colors = colors;
        this.itemClickedListener = itemClickedListener;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        ColorSet color = colors.get(position);
        final FrameLayout frame = getFrameLayout();
        final ColorPreviewButton button = getColorPreviewButton();
        button.setInnerColor(color.color);
        button.setOuterColor(color.colorAccent);
        frame.addView(button);

        button.setOnClickListener(v -> {
            button.setTag(position);
            itemClickedListener.onClick(button);
        });

        return frame;
    }

    protected FrameLayout getFrameLayout() {
        return new FrameLayout(getContext());
    }

    protected ColorPreviewButton getColorPreviewButton() {
        return new ColorPreviewButton(getContext());
    }

}
