package xyz.klinker.messenger.shared.view

import android.content.Context
import android.preference.PreferenceCategory
import android.support.v14.preference.SwitchPreference
import android.util.AttributeSet

import xyz.klinker.messenger.shared.R

class MaterialPreferenceCategory @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : PreferenceCategory(context, attrs, defStyleAttr) {

    init {
        layoutResource = R.layout.preference_category_card
    }
}
