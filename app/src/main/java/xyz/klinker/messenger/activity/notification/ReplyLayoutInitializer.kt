package xyz.klinker.messenger.activity.notification

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.support.v4.view.GestureDetectorCompat
import android.text.Html
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.pojo.BaseTheme
import xyz.klinker.messenger.shared.util.ContactImageCreator
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.DensityUtil

@Suppress("DEPRECATION")
class ReplyLayoutInitializer(private val activity: MarshmallowReplyActivity, private val dataProvider: ReplyDataProvider, private val animator: ReplyAnimators) {

    private val content: View by lazy { activity.findViewById<View>(android.R.id.content) }
    private val image: CircleImageView by lazy { activity.findViewById<View>(R.id.image) as CircleImageView }
    private val messagesInitial: LinearLayout by lazy { activity.findViewById<View>(R.id.messages_initial) as LinearLayout }
    private val messagesInitialHolder: LinearLayout by lazy { activity.findViewById<View>(R.id.messages_initial_holder) as LinearLayout }
    private val messagesMore: LinearLayout by lazy { activity.findViewById<View>(R.id.messages_more) as LinearLayout }
    private val dimBackground: View by lazy { activity.findViewById<View>(R.id.dim_background) }
    private val scrollviewFiller: View by lazy { activity.findViewById<View>(R.id.scrollview_filler) }
    private val scrollView: ScrollView by lazy { activity.findViewById<View>(R.id.scroll_view) as ScrollView }
    private val sendBar: LinearLayout by lazy { activity.findViewById<View>(R.id.send_bar) as LinearLayout }

    fun displayMessages() {
        for (i in dataProvider.messages.indices) {
            if (i < ReplyDataProvider.PREV_MESSAGES_DISPLAYED) {
                messagesInitial.addView(generateMessageTextView(dataProvider.messages[i]), 0)
            } else {
                messagesMore.addView(generateMessageTextView(dataProvider.messages[i]), 0)
            }
        }

        scrollviewFiller.post {
            resizeDismissibleView()
            scrollView.post { showScrollView() }
        }
    }

    fun showContactImage() {
        if (dataProvider.conversation?.imageUri == null) {
            when {
                ContactUtils.shouldDisplayContactLetter(dataProvider.conversation) ->
                    image.setImageBitmap(ContactImageCreator.getLetterPicture(activity, dataProvider.conversation))
                Settings.useGlobalThemeColor -> image.setImageDrawable(ColorDrawable(Settings.mainColorSet.color))
                else -> image.setImageDrawable(ColorDrawable(dataProvider.conversation!!.colors.color))
            }
        } else {
            Glide.with(activity).load(Uri.parse(dataProvider.conversation?.imageUri)).into(image)
        }
    }

    private fun showScrollView() {
        scrollView.scrollTo(0, scrollView.bottom)
        scrollView.visibility = View.VISIBLE

        animator.bounceIn()
    }

    fun setupBackgroundComponents() {
        if (Settings.baseTheme === BaseTheme.BLACK) {
            messagesInitialHolder.setBackgroundColor(Color.BLACK)
            messagesMore.setBackgroundColor(Color.BLACK)
        }

        dimBackground.setOnClickListener({ activity.onBackPressed() })
        scrollviewFiller.setOnClickListener({ activity.onBackPressed() })

        messagesInitialHolder.setOnClickListener {
            activity.onBackPressed()

            val intent = Intent(activity, MessengerActivity::class.java)
            intent.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID, dataProvider.conversationId)
            intent.putExtra(MessengerActivityExtras.EXTRA_FROM_NOTIFICATION, true)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            activity.startActivity(intent)
        }

        messagesMore.setOnClickListener { messagesInitialHolder.performClick() }

        if (activity.resources.getBoolean(R.bool.is_tablet)) {
            scrollView.layoutParams.width = DensityUtil.toDp(activity, 418)
        }

        val detectorCompat = GestureDetectorCompat(activity, FlingOutGestureDetector(activity))
        scrollView.setOnTouchListener({ _, motionEvent ->
            detectorCompat.onTouchEvent(motionEvent)
            false
        })
    }

    private fun resizeDismissibleView() {
        val dismissableParams = scrollviewFiller.layoutParams
        dismissableParams.height = content.height - sendBar.height - messagesInitialHolder.height
        scrollviewFiller.layoutParams = dismissableParams
    }

    private fun generateMessageTextView(message: Message): TextView {
        val tv = TextView(activity)
        tv.maxLines = 3
        tv.ellipsize = TextUtils.TruncateAt.END
        tv.setTextColor(activity.resources.getColor(R.color.primaryText))

        val string = if (message.type == Message.TYPE_RECEIVED) {
            "<b>" + (if (message.from != null) message.from else dataProvider.conversation?.title) + ":</b> "
        } else {
            activity.getString(R.string.you) + ": "
        }

        tv.text = Html.fromHtml(string + when {
            MimeType.isAudio(message.mimeType!!) -> "<i>" + activity.getString(R.string.audio_message) + "</i>"
            MimeType.isVideo(message.mimeType!!) -> "<i>" + activity.getString(R.string.video_message) + "</i>"
            MimeType.isVcard(message.mimeType!!) -> "<i>" + activity.getString(R.string.contact_card) + "</i>"
            MimeType.isStaticImage(message.mimeType) -> "<i>" + activity.getString(R.string.picture_message) + "</i>"
            message.mimeType == MimeType.IMAGE_GIF -> "<i>" + activity.getString(R.string.gif_message) + "</i>"
            MimeType.isExpandedMedia(message.mimeType) -> "<i>" + activity.getString(R.string.media) + "</i>"
            else -> message.data
        })

        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        params.topMargin = DensityUtil.toDp(activity, 4)
        params.bottomMargin = params.topMargin
        tv.layoutParams = params

        return tv
    }
}