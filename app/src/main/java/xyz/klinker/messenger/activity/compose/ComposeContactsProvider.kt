package xyz.klinker.messenger.activity.compose

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.MultiAutoCompleteTextView
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.ex.chips.BaseRecipientAdapter
import com.android.ex.chips.RecipientEditTextView
import com.android.ex.chips.recipientchip.DrawableRecipientChip
import com.roughike.bottombar.BottomBar
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.ContactAdapter
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.ImageContact
import xyz.klinker.messenger.shared.util.*
import xyz.klinker.messenger.shared.util.listener.ContactClickedListener
import java.util.ArrayList

@Suppress("DEPRECATION")
class ComposeContactsProvider(private val activity: ComposeActivity) : ContactClickedListener {

    val contactEntry: RecipientEditTextView by lazy { activity.findViewById<View>(R.id.contact_entry) as RecipientEditTextView }
    private val bottomNavigation: BottomBar by lazy { activity.findViewById<View>(R.id.bottom_navigation) as BottomBar }
    private val recyclerView: RecyclerView by lazy { activity.findViewById<View>(R.id.recent_contacts) as RecyclerView }
    private val loadingSpinner: ProgressBar by lazy { activity.findViewById(R.id.loading) as ProgressBar }

    private var conversations: List<Conversation>? = null
    private var groups: MutableList<Conversation>? = null
    private var allContacts: List<Conversation>? = null

    fun getRecipients(): Array<DrawableRecipientChip> = contactEntry.recipients
    fun hasContacts() = contactEntry.text.isNotEmpty()

    fun setupViews() {
        ColorUtils.changeRecyclerOverscrollColors(recyclerView, Settings.mainColorSet.color)
        ColorUtils.setCursorDrawableColor(contactEntry, Settings.mainColorSet.colorAccent)

        val adapter = BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, activity)
        adapter.isShowMobileOnly = Settings.mobileOnly

        contactEntry.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        contactEntry.highlightColor = Settings.mainColorSet.colorAccent
        contactEntry.setAdapter(adapter)
        contactEntry.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                activity.sender.fab.performClick()
            }

            false
        }

        if (Settings.isCurrentlyDarkTheme && Settings.mainColorSet.colorAccent == Color.BLACK) {
            bottomNavigation.setActiveTabColor(Color.WHITE)
        } else {
            bottomNavigation.setActiveTabColor(Settings.mainColorSet.colorAccent)
        }

        bottomNavigation.setOnTabSelectListener { item ->
            when (item) {
                R.id.tab_recents -> displayRecents()
                R.id.tab_groups -> displayGroups()
                R.id.tab_all_contacts -> displayAllContacts()
            }
        }

        if (!ColorUtils.isColorDark(Settings.mainColorSet.color)) {
            contactEntry.setTextColor(ColorStateList.valueOf(activity.resources.getColor(R.color.lightToolbarTextColor)))
            contactEntry.setHintTextColor(ColorStateList.valueOf(activity.resources.getColor(R.color.lightToolbarTextColor)))
        }

        contactEntry.requestFocus()
        displayRecents()
    }

    fun getPhoneNumberFromContactEntry(): String {
        val chips = getRecipients()
        val phoneNumbers = StringBuilder()

        if (chips.isNotEmpty()) {
            for (i in chips.indices) {
                phoneNumbers.append(PhoneNumberUtils.clearFormatting(chips[i].entry.destination))
                if (i != chips.size - 1) {
                    phoneNumbers.append(", ")
                }
            }
        } else {
            phoneNumbers.append(contactEntry.text.toString())
        }

        return phoneNumbers.toString()
    }

    fun toggleMobileOnly(mobileOnly: Boolean) {
        contactEntry.adapter.isShowMobileOnly = mobileOnly
        contactEntry.adapter.notifyDataSetChanged()
    }

    private fun displayRecents() {
        val handler = Handler()
        loadingSpinner.visibility = View.GONE

        Thread {
            if (conversations == null) {
                conversations = queryConversations()
            }

            handler.post {
                val adapter = ContactAdapter(if (conversations == null) ArrayList() else conversations!!, this)
                recyclerView.layoutManager = LinearLayoutManager(activity)
                recyclerView.adapter = adapter
            }
        }.start()
    }

    private fun displayGroups() {
        val handler = Handler()

        if (groups == null) {
            val adapter = ContactAdapter(ArrayList(), this)
            recyclerView.layoutManager = LinearLayoutManager(activity)
            recyclerView.adapter = adapter
            loadingSpinner.visibility = View.VISIBLE
        } else {
            loadingSpinner.visibility = View.GONE
        }

        Thread {
            if (groups == null) {
                groups = ContactUtils.queryContactGroups(activity).toMutableList()

                if (conversations != null) {
                    conversations!!.indices
                            .filter { conversations!![it].phoneNumbers!!.contains(", ") }
                            .forEach { groups!!.add(conversations!![it]) }
                }
            }

            handler.post {
                loadingSpinner.visibility = View.GONE

                val adapter = ContactAdapter(if (groups == null) ArrayList() else groups!!, this)
                recyclerView.layoutManager = LinearLayoutManager(activity)
                recyclerView.adapter = adapter
            }
        }.start()
    }

    private fun displayAllContacts() {
        val handler = Handler()

        if (allContacts == null) {
            val adapter = ContactAdapter(ArrayList(), this)
            recyclerView.layoutManager = LinearLayoutManager(activity)
            recyclerView.adapter = adapter
            loadingSpinner.visibility = View.VISIBLE
        } else {
            loadingSpinner.visibility = View.GONE
        }

        Thread {
            if (allContacts == null) {
                val contacts = ContactUtils.queryContacts(activity, DataSource)
                allContacts = contacts
                        .sortedBy { it.name }
                        .map { it as ImageContact}
                        .map {
                            val conversation = Conversation()
                            conversation.title = it.name
                            conversation.phoneNumbers = it.phoneNumber
                            conversation.imageUri = it.image
                            conversation.colors = ColorUtils.getRandomMaterialColor(activity)

                            conversation
                        }
            }

            handler.post {
                loadingSpinner.visibility = View.GONE

                val adapter = ContactAdapter(if (allContacts == null) ArrayList() else allContacts!!, this)
                recyclerView.layoutManager = LinearLayoutManager(activity)
                recyclerView.adapter = adapter
            }
        }.start()
    }

    private fun queryConversations() = DataSource.getAllNonPrivateConversationsAsList(activity)

    // we have a few different cases:
    // 1.) Single recipient (with single number)
    // 2.) Group convo with custom title (1 name, multiple numbers)
    // 3.) Group convo with non custom title (x names, x numbers)
    override fun onClicked(conversation: Conversation) {
        onClicked(conversation.title!!, conversation.phoneNumbers!!, conversation.imageUri)
    }

    fun onClicked(title: String, phoneNumber: String, imageUri: String?) {
        val names = title.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val numbers = phoneNumber.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        try {
            if (names.size == 1 && numbers.size == 1) {
                // Case 1
                if (imageUri == null) {
                    contactEntry.submitItem(title, phoneNumber)
                } else {
                    contactEntry.submitItem(title, phoneNumber, Uri.parse(imageUri))
                }
            } else {
                if (names.size == numbers.size) {
                    // case 3
                    for (i in names.indices) {
                        val name = names[i]
                        val number = numbers[i]
                        val image = ContactUtils.findImageUri(number, activity)

                        if (image != null) {
                            contactEntry.submitItem(name, number, Uri.parse(image + "/photo"))
                        } else {
                            contactEntry.submitItem(name, number)
                        }
                    }
                } else {
                    // case 2
                    for (i in numbers.indices) {
                        val number = numbers[i]
                        val name = ContactUtils.findContactNames(number, activity)
                        val image = ContactUtils.findImageUri(number, activity)

                        if (image != null) {
                            contactEntry.submitItem(name, number, Uri.parse(image + "/photo"))
                        } else {
                            contactEntry.submitItem(name, number)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // no permission for contacts
            PermissionsUtils.startMainPermissionRequest(activity)
        }
    }

}