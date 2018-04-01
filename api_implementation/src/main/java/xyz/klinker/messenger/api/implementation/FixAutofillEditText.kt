package xyz.klinker.messenger.api.implementation

import android.content.Context
import android.os.Build
import android.support.v7.widget.AppCompatEditText
import android.util.AttributeSet
import android.view.View

class FixAutofillEditText : AppCompatEditText {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    // stupid bug on Android 8.0 where they couldn't log in when they are on Android 8.0 and have a password
    // manager set up. Fixed in 8.1+:
    // https://issuetracker.google.com/issues/67675432#comment6
    override fun getAutofillType(): Int {
        return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            View.AUTOFILL_TYPE_NONE
        } else {
            super.getAutofillType()
        }
    }
}