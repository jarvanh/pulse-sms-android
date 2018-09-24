package xyz.klinker.messenger.view

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import xyz.klinker.messenger.activity.share.QuickShareActivity
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.KeyboardLayout
import xyz.klinker.messenger.shared.util.AndroidVersionUtil
import xyz.klinker.messenger.shared.view.emoji.EmojiableEditText

class ImageKeyboardEditText : EmojiableEditText {

    private var commitContentListener: InputConnectionCompat.OnCommitContentListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setCommitContentListener(listener: InputConnectionCompat.OnCommitContentListener) {
        this.commitContentListener = listener
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        val con = super.onCreateInputConnection(outAttrs)
        EditorInfoCompat.setContentMimeTypes(outAttrs, arrayOf("image/gif", "image/png"))

        if (Settings.keyboardLayout === KeyboardLayout.SEND || context is QuickShareActivity) {
            val imeActions = outAttrs.imeOptions and EditorInfo.IME_MASK_ACTION
            if (imeActions and EditorInfo.IME_ACTION_SEND != 0) {
                // clear the existing action
                outAttrs.imeOptions = outAttrs.imeOptions xor imeActions
                // set the DONE action
                outAttrs.imeOptions = outAttrs.imeOptions or EditorInfo.IME_ACTION_SEND
            }
            if (outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) {
                outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION.inv()
            }
        }

        return InputConnectionCompat.createWrapper(con, outAttrs) { inputContentInfo, flags, opts ->
            if (commitContentListener != null) {
                if (AndroidVersionUtil.isAndroidN_MR1 && flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION != 0) {
                    try {
                        inputContentInfo.requestPermission()
                    } catch (e: Exception) {
                        return@createWrapper false
                    }
                }

                commitContentListener?.onCommitContent(
                        inputContentInfo, flags, opts
                )

                return@createWrapper true
            } else {
                return@createWrapper false
            }
        }
    }
}
