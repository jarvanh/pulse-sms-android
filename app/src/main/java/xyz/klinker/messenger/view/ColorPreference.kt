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

package xyz.klinker.messenger.view

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.TypedArray
import android.graphics.drawable.ColorDrawable
import android.preference.Preference
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.ScrollView

import com.larswerkman.lobsterpicker.LobsterPicker
import com.larswerkman.lobsterpicker.sliders.LobsterShadeSlider

import de.hdodenhof.circleimageview.CircleImageView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.ColorPickerAdapter
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.listener.ColorSelectedListener

/**
 * Preference that supports a color picker.
 */
class ColorPreference : Preference {

    private var color: Int = 0
    private var displayNormalize: Boolean = false
    private var view: View? = null
    private var dialog: AlertDialog? = null
    private var colorSelectedListener: ColorSelectedListener? = null

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.color_picker_preference)
            displayNormalize = a.getBoolean(R.styleable.color_picker_preference_display_normalize, false)
            a.recycle()
        } else {
            displayNormalize = false
        }

        color = PreferenceManager.getDefaultSharedPreferences(context).getInt(key, 0)

        summary = ColorUtils.convertToHex(color)
        setOnPreferenceClickListener {
            displayPicker()
            true
        }
    }

    private fun displayPicker() {
        val inflater = LayoutInflater.from(context)

        val dialog = inflater.inflate(R.layout.dialog_color_preference, null, false) as ScrollView
        val picker = dialog.findViewById<View>(R.id.lobsterpicker) as LobsterPicker
        val slider = dialog.findViewById<View>(R.id.shadeslider) as LobsterShadeSlider
        val grid = dialog.findViewById<View>(R.id.color_picker) as GridView

        picker.addDecorator(slider)
        picker.history = color
        picker.color = color

        val colors = ColorUtils.getColors(context)
        val adapter = ColorPickerAdapter(context,
                colors) { view ->
            this@ColorPreference.dialog?.hide()
            val color = colors[view.tag as Int]
            setColor(color.color)

            if (colorSelectedListener != null) {
                colorSelectedListener!!.onColorSelected(color)
            }
        }

        grid.adapter = adapter

        this.dialog = AlertDialog.Builder(context)
                .setView(dialog)
                .setPositiveButton(android.R.string.ok) { _, _ -> setColor(picker.color) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()

        if (displayNormalize) {
            dialog.findViewById<View>(R.id.normalize).visibility = View.VISIBLE
        } else {
            dialog.findViewById<View>(R.id.normalize).visibility = View.GONE
        }

        dialog.post { dialog.scrollTo(0, 0) }
    }

    fun setColorSelectedListener(listener: ColorSelectedListener) {
        colorSelectedListener = listener
    }

    fun setColor(color: Int) {
        this.color = color
        setPreviewView()
        if (onPreferenceChangeListener != null) {
            onPreferenceChangeListener
                    .onPreferenceChange(this@ColorPreference, color)
        }
    }

    public override fun onBindView(view: View) {
        super.onBindView(view)
        this.view = view
        setPreviewView()
    }

    private fun setPreviewView() {
        if (view == null) {
            return
        }

        val widgetFrameView = view!!.findViewById<View>(android.R.id.widget_frame) as LinearLayout
        widgetFrameView.removeAllViews()
        widgetFrameView.visibility = View.VISIBLE
        val circle = LayoutInflater.from(context)
                .inflate(R.layout.preference_color, widgetFrameView, true).findViewById<View>(R.id.color) as CircleImageView
        circle.setImageDrawable(ColorDrawable(color))
        summary = ColorUtils.convertToHex(color)
    }

}
