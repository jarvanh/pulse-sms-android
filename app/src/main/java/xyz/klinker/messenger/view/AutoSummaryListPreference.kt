package xyz.klinker.messenger.view

import android.content.Context
import android.preference.ListPreference
import android.util.AttributeSet

class AutoSummaryListPreference : ListPreference {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        if (positiveResult) {
            summary = summary
        }
    }

    override fun getSummary() = try {
        entries[findIndexOfValue(value)]
    } catch (e: Exception) {
        ""
    }
}