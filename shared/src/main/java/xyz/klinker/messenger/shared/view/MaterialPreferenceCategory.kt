package xyz.klinker.messenger.shared.view

import android.content.Context
import android.preference.PreferenceCategory
import android.support.v14.preference.SwitchPreference
import android.util.AttributeSet

import xyz.klinker.messenger.shared.R

class MaterialPreferenceCategory : PreferenceCategory {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)


    init {
        layoutResource = R.layout.preference_category_card
    }
}
