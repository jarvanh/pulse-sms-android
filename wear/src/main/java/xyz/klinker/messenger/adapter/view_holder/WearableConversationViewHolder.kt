package xyz.klinker.messenger.adapter.view_holder

import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import de.hdodenhof.circleimageview.CircleImageView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessageListActivity
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation

class WearableConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val header: TextView by lazy { itemView.findViewById<View>(R.id.header) as TextView }
    val image: CircleImageView? by lazy { itemView.findViewById<View>(R.id.image) as CircleImageView? }
    val name: TextView by lazy { itemView.findViewById<View>(R.id.name) as TextView }
    val summary: TextView by lazy { itemView.findViewById<View>(R.id.summary) as TextView }
    val imageLetter: TextView by lazy { itemView.findViewById<View>(R.id.image_letter) as TextView }
    val groupIcon: ImageView by lazy { itemView.findViewById<View>(R.id.group_icon) as ImageView }
    private val unreadIndicator: View? by lazy { itemView.findViewById<View>(R.id.unread_indicator) }

    var conversation: Conversation? = null

    val isBold: Boolean
        get() = name.typeface != null && name.typeface.isBold

    val isItalic: Boolean
        get() = name.typeface != null && name.typeface.style == Typeface.ITALIC

    init {
        itemView.setOnClickListener {
            if (conversation != null) {
                MessageListActivity.startActivity(itemView.context, conversation!!.id)

                if (unreadIndicator != null && unreadIndicator!!.visibility == View.VISIBLE) {
                    setTypeface(false, isItalic)
                }
            }
        }
    }

    fun setTypeface(bold: Boolean, italic: Boolean) {
        if (bold) {
            name.setTypeface(Typeface.DEFAULT_BOLD, if (italic) Typeface.ITALIC else Typeface.NORMAL)
            summary.setTypeface(Typeface.DEFAULT_BOLD, if (italic) Typeface.ITALIC else Typeface.NORMAL)

            if (unreadIndicator != null) {
                unreadIndicator!!.visibility = View.VISIBLE
            }

            (unreadIndicator as CircleImageView).setImageDrawable(ColorDrawable(Settings.mainColorSet.color))
        } else {
            name.setTypeface(Typeface.DEFAULT, if (italic) Typeface.ITALIC else Typeface.NORMAL)
            summary.setTypeface(Typeface.DEFAULT, if (italic) Typeface.ITALIC else Typeface.NORMAL)

            if (unreadIndicator != null) {
                unreadIndicator!!.visibility = View.GONE
            }
        }
    }
}
