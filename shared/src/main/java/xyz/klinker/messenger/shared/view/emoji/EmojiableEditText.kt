package xyz.klinker.messenger.shared.view.emoji

import android.content.Context
import android.support.text.emoji.widget.EmojiEditTextHelper
import android.support.v7.widget.AppCompatEditText
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

open class EmojiableEditText : AppCompatEditText {

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        super.setKeyListener(emojiEditTextHelper.getKeyListener(keyListener))
    }

    override fun setKeyListener(keyListener: android.text.method.KeyListener) {
        super.setKeyListener(emojiEditTextHelper.getKeyListener(keyListener))
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        val inputConnection = super.onCreateInputConnection(outAttrs)
        return emojiEditTextHelper.onCreateInputConnection(inputConnection, outAttrs)
    }

    private var mEmojiEditTextHelper: EmojiEditTextHelper? = null
    private val emojiEditTextHelper: EmojiEditTextHelper
        get() {
            if (mEmojiEditTextHelper == null) {
                mEmojiEditTextHelper = EmojiEditTextHelper(this)
            }
            return mEmojiEditTextHelper as EmojiEditTextHelper
        }
}