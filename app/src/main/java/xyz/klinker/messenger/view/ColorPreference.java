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
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.larswerkman.lobsterpicker.LobsterPicker;
import com.larswerkman.lobsterpicker.sliders.LobsterShadeSlider;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ColorPickerAdapter;
import xyz.klinker.messenger.data.ColorSet;
import xyz.klinker.messenger.util.ColorUtils;
import xyz.klinker.messenger.util.listener.ColorSelectedListener;

/**
 * Preference that supports a color picker.
 */
public class ColorPreference extends Preference {

    private int color;
    private boolean displayNormalize;
    private View view;
    private AlertDialog dialog;
    private ColorSelectedListener colorSelectedListener;

    public ColorPreference(Context context) {
        super(context);
        init(null);
    }

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.color_picker_preference);
            displayNormalize = a.getBoolean(R.styleable.color_picker_preference_display_normalize,
                    false);
            a.recycle();
        } else {
            displayNormalize = false;
        }

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
        //noinspection AndroidLintInflateParams
        final ScrollView dialog = (ScrollView) inflater.inflate(R.layout.dialog_color_preference,
                null, false);
        final LobsterPicker picker = (LobsterPicker) dialog.findViewById(R.id.lobsterpicker);
        LobsterShadeSlider slider = (LobsterShadeSlider) dialog.findViewById(R.id.shadeslider);
        GridView grid = (GridView) dialog.findViewById(R.id.color_picker);

        picker.addDecorator(slider);
        picker.setHistory(color);
        picker.setColor(color);

        final List<ColorSet> colors = ColorUtils.getColors(getContext());
        ColorPickerAdapter adapter = new ColorPickerAdapter(getContext(),
                colors, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ColorPreference.this.dialog.hide();
                ColorSet color = colors.get((int) view.getTag());
                setColor(color.color);

                if (colorSelectedListener != null) {
                    colorSelectedListener.onColorSelected(color);
                }
            }
        });

        grid.setAdapter(adapter);

        this.dialog = new AlertDialog.Builder(getContext())
                .setView(dialog)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setColor(picker.getColor());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

        if (displayNormalize) {
            dialog.findViewById(R.id.normalize).setVisibility(View.VISIBLE);
        } else {
            dialog.findViewById(R.id.normalize).setVisibility(View.GONE);
        }

        dialog.post(new Runnable() {
            @Override
            public void run() {
                dialog.scrollTo(0, 0);
            }
        });
    }

    public void setColorSelectedListener(ColorSelectedListener listener) {
        colorSelectedListener = listener;
    }

    public void setColor(int color) {
        this.color = color;
        setPreviewView();
        if (getOnPreferenceChangeListener() != null) {
            getOnPreferenceChangeListener()
                    .onPreferenceChange(ColorPreference.this, color);
        }
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
