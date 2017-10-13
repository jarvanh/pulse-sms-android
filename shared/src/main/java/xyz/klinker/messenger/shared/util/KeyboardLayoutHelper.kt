package xyz.klinker.messenger.shared.util

import android.content.Context
import android.support.annotation.VisibleForTesting
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.EditText

import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.KeyboardLayout

class KeyboardLayoutHelper
internal constructor(settings: Settings) {

    constructor() : this(Settings)

    private val layout: KeyboardLayout = settings.keyboardLayout

    fun applyLayout(editText: EditText) {
        var inputType = editText.inputType
        val imeOptions = editText.imeOptions

        when (layout) {
            KeyboardLayout.DEFAULT -> inputType = inputType or InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
            KeyboardLayout.SEND -> { }
            KeyboardLayout.ENTER -> { }
        }

        editText.inputType = inputType
        editText.imeOptions = imeOptions
    }
}
