package xyz.klinker.messenger.activity.share

import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.MultiAutoCompleteTextView
import com.android.ex.chips.BaseRecipientAdapter
import com.android.ex.chips.RecipientEditTextView
import xyz.klinker.android.floating_tutorial.FloatingTutorialActivity
import xyz.klinker.android.floating_tutorial.TutorialPage
import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.KeyboardLayoutHelper

class QuickShareActivity : FloatingTutorialActivity() {
    override fun getPages(): List<TutorialPage> = listOf(QuickSharePage(this))
}

class QuickSharePage(val activity: QuickShareActivity) : TutorialPage(activity) {

    val contactEntry: RecipientEditTextView by lazy { findViewById<RecipientEditTextView>(R.id.contact_entry) }
    val messageEntry: EditText by lazy { findViewById<EditText>(R.id.message_entry) }

    override fun initPage() {
        setContentView(R.layout.page_quick_share)
        setNextButtonText(R.string.send)
        setBackgroundColorResource(R.color.background)

        if (Settings.isCurrentlyDarkTheme) {
            Handler().post { setProgressIndicatorColorResource(R.color.tutorial_dark_background_indicator) }
        }

        findViewById<View>(R.id.top_background).setBackgroundColor(Settings.mainColorSet.color)
        ColorUtils.setCursorDrawableColor(messageEntry, Settings.mainColorSet.colorAccent)
        KeyboardLayoutHelper.applyLayout(messageEntry)
        prepareContactEntry()

        val sendButton = findViewById<View>(R.id.tutorial_next_button)
        sendButton.setOnClickListener {
            if (contactEntry.recipients.isNotEmpty() && messageEntry.text.isNotEmpty()) {
                ShareSender(this).sendMessage()
                activity.finishAnimated()
            }
        }

        messageEntry.setOnEditorActionListener({ _, actionId, keyEvent ->
            if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN &&
                    keyEvent.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_SEND) {
                sendButton.performClick()
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        })
    }

    private fun prepareContactEntry() {
        val adapter = BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, context)
        adapter.isShowMobileOnly = Settings.mobileOnly

        contactEntry.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        contactEntry.highlightColor = Settings.mainColorSet.colorAccent
        contactEntry.setAdapter(adapter)
    }
}