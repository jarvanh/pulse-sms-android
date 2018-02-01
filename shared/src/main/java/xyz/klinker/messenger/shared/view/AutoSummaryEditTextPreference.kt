package xyz.klinker.messenger.shared.view

import android.content.Context
import android.preference.EditTextPreference
import android.util.AttributeSet

class AutoSummaryEditTextPreference : EditTextPreference {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        if (positiveResult) {
            summary = summary
        }
    }

    override fun getSummary(): CharSequence? {
        return text
    }
}
