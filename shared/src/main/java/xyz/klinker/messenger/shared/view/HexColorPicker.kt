package xyz.klinker.messenger.shared.view

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import xyz.klinker.messenger.shared.R

class HexColorPicker(context: Context) : ColorPicker(context as Activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val hexCode = findViewById<EditText>(R.id.hexCode)
        hexCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (hexCode.text.length == 6) {
                    ignoreSetText = true
                    Handler().postDelayed({ ignoreSetText = false }, 250)

                    updateColorView(hexCode.text.toString())
                }
            }
        })
    }
}