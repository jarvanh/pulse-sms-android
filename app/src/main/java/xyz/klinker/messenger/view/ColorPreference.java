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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.larswerkman.lobsterpicker.LobsterPicker;
import com.larswerkman.lobsterpicker.sliders.LobsterShadeSlider;

import de.hdodenhof.circleimageview.CircleImageView;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.util.ColorUtils;

/**
 * Preference that supports a color picker.
 */
public class ColorPreference extends Preference {

    private int color;
    private View view;

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

        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                displayPicker();
                return true;
            }
        });
    }

    private void displayPicker() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialog = inflater.inflate(R.layout.dialog_color_preference, null, false);
        final LobsterPicker picker = (LobsterPicker) dialog.findViewById(R.id.lobsterpicker);
        LobsterShadeSlider slider = (LobsterShadeSlider) dialog.findViewById(R.id.shadeslider);

        picker.addDecorator(slider);
        picker.setHistory(color);
        picker.setColor(color);

        new AlertDialog.Builder(getContext())
                .setView(dialog)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        color = picker.getColor();
                        setPreviewView();
                        if (getOnPreferenceChangeListener() != null) {
                            getOnPreferenceChangeListener()
                                    .onPreferenceChange(ColorPreference.this, color);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        this.view = view;
        setPreviewView();
    }

    private void setPreviewView() {
        LinearLayout widgetFrameView = ((LinearLayout) view.findViewById(android.R.id.widget_frame));
        widgetFrameView.removeAllViews();
        widgetFrameView.setVisibility(View.VISIBLE);
        CircleImageView circle = (CircleImageView) LayoutInflater.from(getContext())
                .inflate(R.layout.preference_color, widgetFrameView, true).findViewById(R.id.color);
        circle.setImageDrawable(new ColorDrawable(color));
        setSummary(ColorUtils.convertToHex(color));
    }

}
