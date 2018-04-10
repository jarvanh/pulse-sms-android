package xyz.klinker.messenger.activity.main

import android.content.*
import android.net.Uri
import android.os.Handler
import android.provider.ContactsContract
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
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

class MainNavigationMessageListActionDelegate(private val activity: MessengerActivity) {

    private val navController
        get() = activity.navController
    private val conversationActionDelegate
        get() = navController.conversationActionDelegate

    fun callContact(): Boolean {
        if (navController.isConversationListExpanded()) {
            val uri = "tel:" + navController.conversationListFragment!!.expandedItem!!.conversation!!.phoneNumbers!!
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse(uri)

            try {
                activity.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(activity, R.string.no_apps_found, Toast.LENGTH_SHORT).show()
            } catch (e: SecurityException) {
                Toast.makeText(activity, R.string.you_denied_permission, Toast.LENGTH_SHORT).show()
            }

            return true
        } else if (navController.otherFragment is ConversationListFragment) {
            val frag = navController.otherFragment as ConversationListFragment
            if (frag.isExpanded) {
                val uri = "tel:" + frag.expandedItem!!.conversation!!.phoneNumbers!!
                val intent = Intent(Intent.ACTION_CALL)
                intent.data = Uri.parse(uri)

                try {
                    activity.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(activity, R.string.no_apps_found, Toast.LENGTH_SHORT).show()
                } catch (e: SecurityException) {
                    Toast.makeText(activity, R.string.you_denied_permission, Toast.LENGTH_SHORT).show()
                }

                return true
            }
        }

        return false
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
                override fun onClicked(title: String, phoneNumber: String, imageUri: String?) {
                    var intent: Intent?

                    try {
                        intent = Intent(Intent.ACTION_VIEW)
                        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,
                                ContactUtils.findContactId(phoneNumber,activity).toString())
                        intent!!.data = uri
                    } catch (e: NoSuchElementException) {
                        e.printStackTrace()
                        try {
                            intent = Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT)
                            intent!!.data = Uri.parse("tel:" + phoneNumber)
                        } catch (ex: ActivityNotFoundException) {
                            intent = null
                        }

                    }

                    if (intent != null) {
                        activity.startActivity(intent)
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
                    val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,
                            ContactUtils.findContactId(conversation.phoneNumbers!!, activity).toString())
                    intent!!.data = uri
                } catch (e: NoSuchElementException) {
                    e.printStackTrace()
                    try {
                        intent = Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT)
                        intent!!.data = Uri.parse("tel:" + conversation.phoneNumbers!!)
                    } catch (ex: ActivityNotFoundException) {
                        intent = null
                    }

                }

                try {
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
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
                        clipboard!!.primaryClip = clip
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

            BlacklistFragment.addBlacklist(activity, conversation!!.phoneNumbers, {
                val position = fragment.expandedItem!!.adapterPosition
                fragment.expandedItem!!.itemView.performClick()

                try {
                    val adapter = fragment.recyclerView.adapter as ConversationListAdapter
                    adapter.archiveItem(position)
                    adapter.notifyItemRemoved(position)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

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
            conversationActionDelegate.displayFragmentWithBackStack(
                    ScheduledMessagesFragment.newInstance(conversation!!.title!!, conversation.phoneNumbers!!))
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