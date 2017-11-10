package xyz.klinker.messenger.activity.share

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.MultiAutoCompleteTextView
import com.android.ex.chips.BaseRecipientAdapter
import com.android.ex.chips.RecipientEditTextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import xyz.klinker.android.floating_tutorial.FloatingTutorialActivity
import xyz.klinker.android.floating_tutorial.TutorialPage
import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.KeyboardLayoutHelper

class QuickShareActivity : FloatingTutorialActivity() {
    private val page: QuickSharePage by lazy { QuickSharePage(this) }
    override fun getPages() = listOf(page)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ShareIntentHandler(page).handle(intent)
    }
}

class QuickSharePage(val activity: QuickShareActivity) : TutorialPage(activity) {

    val contactEntry: RecipientEditTextView by lazy { findViewById<RecipientEditTextView>(R.id.contact_entry) }
    val messageEntry: EditText by lazy { findViewById<EditText>(R.id.message_entry) }
    private val imagePreview: ImageView by lazy { findViewById<ImageView>(R.id.attached_image) }

    var mediaData: String? = null
    var mimeType: String? = null

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
            if (contactEntry.recipients.isNotEmpty() && ShareSender(this).sendMessage()) {
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

    fun setContacts(phoneNumbers: List<String>) {
        for (number in phoneNumbers) {
            val name = ContactUtils.findContactNames(number, activity)
            val image = ContactUtils.findImageUri(number, activity)

            if (image != null) {
                contactEntry.post { contactEntry.submitItem(name, number, Uri.parse("$image/photo")) }
            } else {
                contactEntry.post { contactEntry.submitItem(name, number) }
            }
        }
    }

    fun setData(data: String, mimeType: String) {
        when {
            mimeType == MimeType.TEXT_PLAIN -> messageEntry.setText(data)
            mimeType == MimeType.IMAGE_GIF -> Glide.with(activity).asGif().apply(RequestOptions().centerCrop()).load(data).into(imagePreview)
            MimeType.isStaticImage(mimeType) -> Glide.with(activity).asBitmap().apply(RequestOptions().centerCrop()).load(data).into(imagePreview)
            MimeType.isVideo(mimeType) -> {
                imagePreview.setImageResource(R.drawable.ic_play_sent)
                imagePreview.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.primaryText))
            }
            MimeType.isAudio(mimeType) -> {
                imagePreview.setImageResource(R.drawable.ic_audio_sent)
                imagePreview.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.primaryText))
            }
        }

        if (mimeType != MimeType.TEXT_PLAIN) {
            imagePreview.visibility = View.VISIBLE

            this.mediaData = data
            this.mimeType = mimeType
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