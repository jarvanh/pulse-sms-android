package xyz.klinker.messenger.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.MultiAutoCompleteTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.ex.chips.BaseRecipientAdapter
import com.android.ex.chips.RecipientEditTextView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.AttachContactAdapter
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.listener.AttachContactListener

/**
 * View that allows you to select a contact and attach it to a message.
 */
@SuppressLint("ViewConstructor")
class AttachContactView(context: Context, private val listener: AttachContactListener, color: Int) : FrameLayout(context) {

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.view_attach_contact, this, true)

        val contactEntry = findViewById<View>(R.id.contact_entry) as RecipientEditTextView
        contactEntry.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        val baseRecipientAdapter = BaseRecipientAdapter(
                BaseRecipientAdapter.QUERY_TYPE_PHONE, context)

        baseRecipientAdapter.isShowMobileOnly = false
        contactEntry.setAdapter(baseRecipientAdapter)
        contactEntry.setPostSelectedAction {
            val chips = contactEntry.sortedRecipients
            if (chips.isNotEmpty()) {
                val name = chips[0].entry.displayName
                val phone = chips[0].entry.destination

                var firstName = ""
                var lastName = ""

                if (name.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size > 1) {
                    firstName = name.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                    lastName = name.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                }

                listener.onContactAttached(firstName, lastName, phone)
            }
        }

        val recentsList = findViewById<View>(R.id.recycler_view) as RecyclerView
        ColorUtils.changeRecyclerOverscrollColors(recentsList, color)
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recentsList.layoutManager = layoutManager

        val adapter = AttachContactAdapter(listener)
        recentsList.adapter = adapter

        val ui = Handler()
        Thread {
            val conversations = DataSource.getUnarchivedConversationsAsList(context)
            ui.post { adapter.setContacts(conversations) }
        }.start()
    }
}
