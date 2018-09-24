/*
 * Copyright (C) 2017 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.fragment

import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.InviteFriendsAdapter
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.util.*
import xyz.klinker.messenger.shared.util.listener.ContactClickedListener
import java.util.*

/**
 * Fragment for inviting friends to the app.
 */
class InviteFriendsFragment : Fragment(), ContactClickedListener {

    private val list: RecyclerView by lazy { view!!.findViewById<View>(R.id.list) as RecyclerView }
    private val fab: FloatingActionButton by lazy { view!!.findViewById<View>(R.id.fab) as FloatingActionButton }
    private val progress: ProgressBar by lazy { view!!.findViewById<View>(R.id.progress) as ProgressBar }
    private var phoneNumbers = mutableListOf<String>()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_invite_friends, parent, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.layoutManager = LinearLayoutManager(activity)

        fab.hide()
        fab.setOnClickListener { sendMessage() }

        loadContacts()
    }

    private fun loadContacts() {
        val handler = Handler()
        Thread {
            val cursor = try {
                activity?.contentResolver
                        ?.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(ContactsContract.CommonDataKinds.Phone._ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null,
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")
            } catch (e: SecurityException) {
                null
            }

            val contacts = ArrayList<Conversation>()
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        val conversation = Conversation()
                        conversation.title = cursor.getString(1)
                        conversation.phoneNumbers = PhoneNumberUtils
                                .clearFormatting(cursor.getString(2))
                        conversation.imageUri = ContactUtils
                                .findImageUri(conversation.phoneNumbers, activity!!)
                        conversation.simSubscriptionId = -1
                        conversation.folderId = -1

                        val image = ImageUtils.getContactImage(conversation.imageUri, activity)
                        if (image == null) {
                            conversation.imageUri = null
                        } else {
                            image.recycle()
                        }

                        if (contacts.size == 0 || conversation.title != contacts[contacts.size - 1].title) {
                            contacts.add(conversation)
                        }
                    } catch (e: NullPointerException) {
                        return@Thread
                    }

                } while (cursor.moveToNext())
            }

            CursorUtil.closeSilent(cursor)

            handler.post {
                if (activity != null) {
                    setContacts(contacts)
                }
            }
        }.start()
    }

    private fun setContacts(contacts: List<Conversation>) {
        progress.visibility = View.GONE
        list.adapter = InviteFriendsAdapter(contacts, this, phoneNumbers)
    }

    override fun onClicked(conversation: Conversation) {
        val phoneNumber = conversation.phoneNumbers!!

        if (phoneNumbers.contains(phoneNumber)) {
            phoneNumbers.remove(phoneNumber)
        } else {
            phoneNumbers.add(phoneNumber)
        }

        if (phoneNumbers.size > 0) {
            fab.show()
        } else {
            fab.hide()
        }
    }

    private fun sendMessage() {
        if (phoneNumbers.size > 10) {
            unlockFreeMonth()
        }

        for (number in phoneNumbers) {
            if (activity != null) {
                SendUtils().send(activity!!, getString(R.string.invite_friends_sms), number)
            }
        }

        activity?.onBackPressed()
    }

    private fun unlockFreeMonth() {
        // TODO
    }

}
