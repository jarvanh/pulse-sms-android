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

package xyz.klinker.messenger.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

import de.hdodenhof.circleimageview.CircleImageView;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.util.ColorUtils;

/**
 * Preference that supports a color picker.
 */
public class ColorPreference extends Preference {

    private int color;

    public ColorPreference(Context context) {
        super(context);
        init();
    }

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        color = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(getKey(), 0);
        setSummary(ColorUtils.convertToHex(color));
        setWidgetLayoutResource(R.layout.preference_color);
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        CircleImageView circle = (CircleImageView) view.findViewById(R.id.color);
        circle.setImageDrawable(new ColorDrawable(color));
    }

}
