package xyz.klinker.messenger.adapter.message

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.fragment.app.FragmentActivity
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.klinker.android.link_builder.applyLinks
import xyz.klinker.android.article.ArticleIntent
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper
import xyz.klinker.messenger.fragment.message.MessageListFragment
import xyz.klinker.messenger.fragment.bottom_sheet.LinkLongClickFragment
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.PhoneNumberUtils
import xyz.klinker.messenger.shared.util.Regex
import xyz.klinker.messenger.shared.util.media.parsers.ArticleParser

@Suppress("DEPRECATION")
class MessageLinkApplier(private val fragment: MessageListFragment, private val accentColor: Int, private val receivedColor: Int) {

    private val activity: FragmentActivity? by lazy { fragment.activity }

    fun apply(holder: MessageViewHolder, message: Message, backgroundColor: Int) {
        val linkColor = if (message.type == Message.TYPE_RECEIVED) {
            holder.message!!.currentTextColor
        } else accentColor

        if (holder.message!!.context == null) {
            return
        }

        holder.message?.movementMethod = TouchableMovementMethod()
        holder.message?.applyLinks(
                buildEmailsLink(holder, linkColor),
                buildWebUrlsLink(holder, linkColor),
                buildPhoneNumbersLink(holder, linkColor)
        )
    }

    private fun buildEmailsLink(holder: MessageViewHolder, linkColor: Int): Link {
        val emails = Link(Patterns.EMAIL_ADDRESS)
        emails.textColor = linkColor
        emails.highlightAlpha = .4f
        emails.setOnClickListener { clickedText ->
            val email = arrayOf(clickedText)
            val uri = Uri.parse("mailto:$clickedText")

            val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
            emailIntent.putExtra(Intent.EXTRA_EMAIL, email)
            try {
                holder.message!!.context.startActivity(emailIntent)
            } catch (e: ActivityNotFoundException) {
            }
        }

        return emails
    }

    private fun buildWebUrlsLink(holder: MessageViewHolder, linkColor: Int): Link {
        val urls = Link(Regex.WEB_URL)
        urls.textColor = linkColor
        urls.highlightAlpha = .4f

        urls.setOnLongClickListener { clickedText ->
            val link = if (!clickedText.startsWith("http")) {
                "http://$clickedText"
            } else clickedText

            val bottomSheet = LinkLongClickFragment()
            bottomSheet.setColors(receivedColor, accentColor)
            bottomSheet.setLink(link)
            bottomSheet.show(activity?.supportFragmentManager!!, "")
        }

        urls.setOnClickListener { clickedText ->
            if (fragment.multiSelect.isSelectable) {
                holder.messageHolder?.performClick()
                return@setOnClickListener
            }

            val link = if (!clickedText.startsWith("http")) {
                "http://$clickedText"
            } else clickedText

            if (skipInternalBrowser(link) || !Settings.internalBrowser) {
                val url = Intent(Intent.ACTION_VIEW)
                url.data = Uri.parse(clickedText)
                try {
                    holder.itemView.context.startActivity(url)
                } catch (e: Exception) {
                    AnalyticsHelper.caughtForceClose(holder.itemView.context, "couldn't start link click: $clickedText", e)
                }
            } else {
                val intent = ArticleIntent.Builder(holder.itemView.context, ArticleParser.ARTICLE_API_KEY)
                        .setTheme(if (Settings.isCurrentlyDarkTheme(holder.itemView.context)) ArticleIntent.THEME_DARK else ArticleIntent.THEME_LIGHT)
                        .setToolbarColor(receivedColor)
                        .setAccentColor(accentColor)
                        .setTextSize(Settings.mediumFont + 1)
                        .build()

                intent.launchUrl(holder.itemView.context, Uri.parse(link))
            }
        }

        return urls
    }

    private fun buildPhoneNumbersLink(holder: MessageViewHolder, linkColor: Int): Link {
        return Link(Regex.PHONE)
                .setTextColor(linkColor)
                .setHighlightAlpha(.4f)
                .setOnClickListener {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse("tel:" + PhoneNumberUtils.clearFormatting(it))

                    try {
                        holder.message!!.context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                    }
                }
    }

    private fun skipInternalBrowser(link: String): Boolean {
        val list = listOf("youtube", "maps.google", "photos.app.goo")
        for (item in list) {
            if (link.contains(item)) {
                return true
            }
        }

        return false
    }
}