package xyz.klinker.messenger.shared.view.emoji

import android.content.Context
import android.support.text.emoji.widget.EmojiTextViewHelper
import android.support.v7.widget.AppCompatTextView
import android.text.InputFilter
import android.util.AttributeSet

class EmojiableTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatTextView(context, attrs, defStyleAttr) {

    private var helper: EmojiTextViewHelper? = null
    private val emojiHelper: EmojiTextViewHelper
        get() {
            if (helper == null) {
                helper = EmojiTextViewHelper(this)
            }
            return helper as EmojiTextViewHelper
        }

    init {
        emojiHelper.updateTransformationMethod()
    }

    override fun setFilters(filters: Array<InputFilter>) {
        super.setFilters(emojiHelper.getFilters(filters))
    }

    override fun setAllCaps(allCaps: Boolean) {
        super.setAllCaps(allCaps)
        emojiHelper.setAllCaps(allCaps)
    }

}