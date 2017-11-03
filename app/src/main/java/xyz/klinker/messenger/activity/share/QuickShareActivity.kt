package xyz.klinker.messenger.activity.share

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.EditText
import android.widget.MultiAutoCompleteTextView
import com.android.ex.chips.BaseRecipientAdapter
import com.android.ex.chips.RecipientEditTextView
import xyz.klinker.android.floating_tutorial.FloatingTutorialActivity
import xyz.klinker.android.floating_tutorial.TutorialPage
import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.ColorUtils

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
        prepareContactEntry()

        findViewById<View>(R.id.tutorial_next_button).setOnClickListener {
            if (contactEntry.recipients.isNotEmpty() && messageEntry.text.isNotEmpty()) {
                ShareSender(this).sendMessage()
                activity.finishAnimated()
            }
        }
    }

    private fun prepareContactEntry() {
        val adapter = BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, context)
        adapter.isShowMobileOnly = Settings.mobileOnly

        contactEntry.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        contactEntry.highlightColor = Settings.mainColorSet.colorAccent
        contactEntry.setAdapter(adapter)


    }
}