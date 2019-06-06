package xyz.klinker.messenger.activity.main

import android.content.*
import android.net.Uri
import android.os.Handler
import android.provider.ContactsContract
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.ContactSettingsActivity
import xyz.klinker.messenger.activity.MediaGridActivity
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.activity.NoLimitMessageListActivity
import xyz.klinker.messenger.activity.compose.ComposeActivity
import xyz.klinker.messenger.activity.compose.ComposeConstants
import xyz.klinker.messenger.adapter.ContactAdapter
import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.fragment.BlacklistFragment
import xyz.klinker.messenger.fragment.ScheduledMessagesFragment
import xyz.klinker.messenger.fragment.conversation.ConversationListFragment
import xyz.klinker.messenger.fragment.message.load.MessageListLoader
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.CursorUtil
import xyz.klinker.messenger.shared.util.ImageUtils
import xyz.klinker.messenger.shared.util.listener.ContactClickedListener
import java.util.ArrayList
import java.util.NoSuchElementException
import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainNavigationMessageListActionDelegate(private val activity: MessengerActivity) {

    private val navController
        get() = activity.navController
    private val conversationActionDelegate
        get() = navController.conversationActionDelegate

    fun callContact(): Boolean {
        val otherFrag = navController.otherFragment
        val conversation = if (navController.isConversationListExpanded()) {
            navController.conversationListFragment!!.expandedItem!!.conversation!!
        } else if (otherFrag is ConversationListFragment && otherFrag.isExpanded) {
            otherFrag.expandedItem!!.conversation!!
        } else {
            return false
        }


//        val uri = try {
//            val id = ContactUtils.findContactId(conversation.phoneNumbers!!, activity)
//            if (id != -1) {
//                "tel:" + ContactUtils.findPhoneNumberByContactId(activity, id.toString())
//            } else {
//                throw IllegalArgumentException("No contact found")
//            }
//        } catch (e: Exception) {
//            "tel:${conversation.phoneNumbers!!}"
//        }
        val uri = "tel:${conversation.phoneNumbers!!}"

        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse(uri)

        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.no_apps_found, Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(activity, R.string.you_denied_permission, Toast.LENGTH_SHORT).show()
        } catch (e: NullPointerException) {
            Toast.makeText(activity, R.string.no_apps_found, Toast.LENGTH_SHORT).show()
        }

        return true
    }

    fun callWithDuo(): Boolean {
        val otherFrag = navController.otherFragment
        val conversation = if (navController.isConversationListExpanded()) {
            navController.conversationListFragment!!.expandedItem!!.conversation!!
        } else if (otherFrag is ConversationListFragment && otherFrag.isExpanded) {
            otherFrag.expandedItem!!.conversation!!
        } else {
            return false
        }

        return try {
            val intent = activity.packageManager.getLaunchIntentForPackage("com.google.android.apps.tachyon")
            intent!!.data = Uri.parse("tel:${conversation.phoneNumbers!!}")
            activity.startActivity(intent)
            true
        } catch (e: Exception) {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.tachyon")))
            false
        }
    }

    internal fun viewContact(): Boolean {
        var conversation: Conversation? = null

        if (navController.isConversationListExpanded()) {
            conversation = navController.conversationListFragment!!.expandedItem!!.conversation
        } else if (navController.otherFragment is ConversationListFragment) {
            val frag = navController.otherFragment as ConversationListFragment
            if (frag.isExpanded) {
                conversation = frag.expandedItem!!.conversation
            }
        }

        if (conversation != null) {
            val names = ContactUtils.findContactNames(conversation.phoneNumbers, activity).split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val numbers = conversation.phoneNumbers!!.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val conversations = ArrayList<Conversation>()

            for (i in numbers.indices) {
                val c = Conversation()
                c.title = if (i < names.size) names[i] else ""
                c.phoneNumbers = numbers[i]
                c.imageUri = ContactUtils.findImageUri(numbers[i], activity)
                c.colors = conversation.colors

                val image = ImageUtils.getContactImage(c.imageUri, activity)
                if (c.imageUri != null && image == null) {
                    c.imageUri = null
                }

                image?.recycle()

                conversations.add(c)
            }

            val adapter = ContactAdapter(conversations, object : ContactClickedListener {
                override fun onClicked(conversation: Conversation) {
                    val phoneNumber = conversation.phoneNumbers!!

                    var intent: Intent?

                    try {
                        intent = Intent(Intent.ACTION_VIEW)
                        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,
                                ContactUtils.findContactId(phoneNumber,activity).toString())
                        intent!!.data = uri
                    } catch (e: NoSuchElementException) {
                        e.printStackTrace()
                        try {
                            intent = Intent(Intent.ACTION_INSERT)
                            intent!!.type = ContactsContract.Contacts.CONTENT_TYPE
                            intent!!.putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber)
                        } catch (ex: ActivityNotFoundException) {
                            intent = null
                        }

                    }

                    if (intent != null) {
                        try {
                            activity.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(activity, R.string.no_apps_found, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            })

            val recyclerView = RecyclerView(activity)
            recyclerView.layoutManager = LinearLayoutManager(activity)
            recyclerView.adapter = adapter

            if (adapter.itemCount == 1) {
                var intent: Intent?

                try {
                    intent = Intent(Intent.ACTION_VIEW)
                    val contactId = ContactUtils.findContactId(conversation.phoneNumbers!!, activity)
                    val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId.toString())
                    intent!!.data = uri
                } catch (e: NoSuchElementException) {
                    e.printStackTrace()

                    try {
                        intent = Intent(Intent.ACTION_INSERT)
                        intent!!.type = ContactsContract.Contacts.CONTENT_TYPE
                        intent!!.putExtra(ContactsContract.Intents.Insert.PHONE, conversation.phoneNumbers!!)
                    } catch (ex: ActivityNotFoundException) {
                        intent = null
                    }
                }

                try {
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(activity, R.string.no_apps_found, Toast.LENGTH_SHORT).show()
                }

            } else {
                val editRecipients = Intent(activity, ComposeActivity::class.java)
                editRecipients.action = ComposeConstants.ACTION_EDIT_RECIPIENTS
                editRecipients.putExtra(ComposeConstants.EXTRA_EDIT_RECIPIENTS_TITLE, conversation.title)
                editRecipients.putExtra(ComposeConstants.EXTRA_EDIT_RECIPIENTS_NUMBERS, conversation.phoneNumbers)

                AlertDialog.Builder(activity)
                        .setView(recyclerView)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNeutralButton(R.string.edit_recipients) { _, _ -> activity.startActivity(editRecipients) }
                        .show()
            }

            return true
        } else {
            return false
        }
    }

    internal fun viewMedia(): Boolean {
        return if (navController.isConversationListExpanded() || navController.isOtherFragmentConvoAndShowing()) {
            val fragment = navController.getShownConversationList()
            val conversationId = fragment!!.expandedId

            val intent = Intent(activity, MediaGridActivity::class.java)
            intent.putExtra(MediaGridActivity.EXTRA_CONVERSATION_ID, conversationId)
            activity.startActivity(intent)
            true
        } else {
            false
        }
    }

    internal fun deleteConversation(): Boolean {
        if (navController.isConversationListExpanded() || navController.isOtherFragmentConvoAndShowing()) {
            val fragment = navController.getShownConversationList()
            val conversationId = fragment!!.expandedId
            fragment.onBackPressed()

            Handler().postDelayed({
                val adapter = fragment.adapter ?: return@postDelayed
                val position = adapter.findPositionForConversationId(conversationId)
                if (position != -1) {
                    adapter.deleteItem(position)
                }
            }, 250)

            return true
        } else {
            return false
        }
    }

    internal fun archiveConversation(): Boolean {
        if (navController.isConversationListExpanded() || navController.isOtherFragmentConvoAndShowing()) {
            val fragment = navController.getShownConversationList()
            val conversationId = fragment!!.expandedId
            fragment.onBackPressed()

            Handler().postDelayed({
                val adapter = fragment.adapter ?: return@postDelayed
                val position = adapter.findPositionForConversationId(conversationId)
                if (position != -1) {
                    adapter.archiveItem(position)
                }
            }, 250)

            return true
        } else {
            return false
        }
    }

    internal fun conversationInformation(): Boolean {
        if (navController.isConversationListExpanded() || navController.isOtherFragmentConvoAndShowing()) {
            val fragment = navController.getShownConversationList()
            val conversation = fragment!!.expandedItem!!.conversation
            val source = DataSource

            val builder = AlertDialog.Builder(activity)
                    .setMessage(source.getConversationDetails(activity, conversation!!))
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(R.string.menu_copy_phone_number) { _, _ ->
                        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                        val clip = ClipData.newPlainText("phone_number", conversation.phoneNumbers)
                        clipboard!!.setPrimaryClip(clip)
                    }

            val messages = source.getMessages(activity, conversation.id)

            if (messages.count > MessageListLoader.MESSAGE_LIMIT) {
                builder.setNegativeButton(R.string.menu_view_full_conversation) { _, _ ->
                    NoLimitMessageListActivity.start(activity, conversation.id) }
            }

            CursorUtil.closeSilent(messages)

            builder.show()
            return true
        } else {
            return false
        }
    }

    internal fun conversationBlacklist(): Boolean {
        return if (navController.isConversationListExpanded() || navController.isOtherFragmentConvoAndShowing()) {
            val fragment = navController.getShownConversationList()
            val conversation = fragment!!.expandedItem!!.conversation

            BlacklistFragment.addBlacklistPhone(activity, conversation!!.phoneNumbers) {
                val position = fragment.expandedItem!!.adapterPosition
                fragment.expandedItem!!.itemView.performClick()

                try {
                    val adapter = fragment.recyclerView.adapter as ConversationListAdapter
                    adapter.archiveItem(position)
                    adapter.notifyItemRemoved(position)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            true
        } else {
            false
        }
    }

    internal fun conversationSchedule(): Boolean {
        return if (navController.isConversationListExpanded() || navController.isOtherFragmentConvoAndShowing()) {
            val fragment = navController.getShownConversationList()
            val conversation = fragment!!.expandedItem!!.conversation
            fragment.expandedItem!!.itemView.performClick()
            activity.clickNavigationItem(R.id.drawer_schedule)

            val messageListFragment = navController.findMessageListFragment()
            if (messageListFragment != null) {
                val messageText = messageListFragment.sendManager.messageEntry.text.toString()
                messageListFragment.sendManager.messageEntry.setText("")

                conversationActionDelegate.displayFragmentWithBackStack(
                        ScheduledMessagesFragment.newInstance(conversation!!.title!!, conversation.phoneNumbers!!, messageText))
            } else {
                conversationActionDelegate.displayFragmentWithBackStack(
                        ScheduledMessagesFragment.newInstance(conversation!!.title!!, conversation.phoneNumbers!!, ""))
            }
        } else {
            false
        }
    }

    internal fun contactSettings(): Boolean {
        return if (navController.isConversationListExpanded() || navController.isOtherFragmentConvoAndShowing()) {
            val fragment = navController.getShownConversationList()
            val conversationId = fragment!!.expandedId
            val intent = Intent(activity, ContactSettingsActivity::class.java)
            intent.putExtra(ContactSettingsActivity.EXTRA_CONVERSATION_ID, conversationId)
            activity.startActivity(intent)
            true
        } else {
            false
        }
    }
}