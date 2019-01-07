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

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.BlacklistAdapter
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Blacklist
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.PhoneNumberUtils
import xyz.klinker.messenger.shared.util.listener.BlacklistClickedListener

/**
 * Fragment for displaying/managing blacklisted contacts.
 */
class BlacklistFragment : Fragment(), BlacklistClickedListener {

    private val fragmentActivity: FragmentActivity? by lazy { activity }

    private val list: RecyclerView by lazy { view!!.findViewById<View>(R.id.list) as RecyclerView }
    private val fab: FloatingActionButton by lazy { view!!.findViewById<View>(R.id.fab) as FloatingActionButton }
    private val emptyView: View by lazy { view!!.findViewById<View>(R.id.empty_view) }

    private var adapter: BlacklistAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_blacklist, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.layoutManager = LinearLayoutManager(fragmentActivity!!)
        fab.setOnClickListener { addBlacklistPhone() }

        emptyView.setBackgroundColor(Settings.mainColorSet.colorLight)
        fab.backgroundTintList = ColorStateList.valueOf(Settings.mainColorSet.colorAccent)
        ColorUtils.changeRecyclerOverscrollColors(list, Settings.mainColorSet.color)

        loadBlacklists()

        if (arguments != null && arguments!!.containsKey(ARG_PHONE_NUMBER)) {
            addBlacklistPhone(fragmentActivity!!, arguments!!.getString(ARG_PHONE_NUMBER), { loadBlacklists() })
        }
    }

    private fun loadBlacklists() {
        val handler = Handler()
        Thread {
            if (fragmentActivity == null) {
                return@Thread
            }

            val blacklists = DataSource.getBlacklistsAsList(fragmentActivity!!)
            handler.post { setBlacklists(blacklists) }
        }.start()
    }

    private fun setBlacklists(blacklists: List<Blacklist>) {
        adapter = BlacklistAdapter(blacklists, this)
        list.adapter = adapter

        if (adapter?.itemCount == 0) {
            emptyView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.GONE
        }
    }

    private fun addBlacklistPhone() {
        val layout = LayoutInflater.from(fragmentActivity).inflate(R.layout.dialog_edit_text, null, false)
        val editText = layout.findViewById<View>(R.id.edit_text) as EditText
        editText.setHint(R.string.blacklist_hint)
        editText.inputType = InputType.TYPE_CLASS_PHONE

        val dialog = AlertDialog.Builder(fragmentActivity!!)
                .setView(layout)
                .setPositiveButton(R.string.add) { _, _ -> addBlacklistPhone(fragmentActivity!!, editText.text.toString(), { loadBlacklists() }) }
                .setNegativeButton(android.R.string.cancel, null)

        if (FeatureFlags.BLACKLIST_PHRASE) {
            dialog.setNeutralButton(R.string.enter_phrase) { _, _ -> addBlacklistPhrase() }
        }

        dialog.show()
    }

    private fun addBlacklistPhrase() {
        val layout = LayoutInflater.from(fragmentActivity).inflate(R.layout.dialog_edit_text, null, false)
        val editText = layout.findViewById<View>(R.id.edit_text) as EditText
        editText.setHint(R.string.blacklist_phrase_hint)

        AlertDialog.Builder(fragmentActivity!!)
                .setView(layout)
                .setPositiveButton(R.string.add) { _, _ -> addBlacklistPhrase(fragmentActivity!!, editText.text.toString()) { loadBlacklists() } }
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.enter_phone) { _, _ -> addBlacklistPhone() }
                .show()
    }

    private fun removePhoneNumber(id: Long, number: String) {
        val message = getString(R.string.remove_blacklist, PhoneNumberUtils.format(number))

        AlertDialog.Builder(fragmentActivity!!)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    DataSource.deleteBlacklist(fragmentActivity!!, id)
                    loadBlacklists()
                }
                .setNegativeButton(android.R.string.no, null)
                .show()
    }

    private fun removePhrase(id: Long, phrase: String) {
        val message = getString(R.string.remove_blacklist, phrase)

        AlertDialog.Builder(fragmentActivity!!)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    DataSource.deleteBlacklist(fragmentActivity!!, id)
                    loadBlacklists()
                }
                .setNegativeButton(android.R.string.no, null)
                .show()
    }

    override fun onClick(position: Int) {
        val blacklist = adapter!!.getItem(position)
        val phoneNumber = blacklist.phoneNumber
        val phrase = blacklist.phrase

        if (!phoneNumber.isNullOrBlank()) {
            removePhoneNumber(blacklist.id, phoneNumber)
        } else if (!phrase.isNullOrBlank()) {
            removePhrase(blacklist.id, phrase)
        }
    }

    companion object {

        private val ARG_PHONE_NUMBER = "phone_number"

        fun newInstance(): BlacklistFragment {
            return BlacklistFragment.newInstance(null)
        }

        fun newInstance(phoneNumber: String?): BlacklistFragment {
            val fragment = BlacklistFragment()
            val args = Bundle()

            if (phoneNumber != null) {
                args.putString(ARG_PHONE_NUMBER, phoneNumber)
            }

            fragment.arguments = args
            return fragment
        }

        fun addBlacklistPhone(fragmentActivity: Activity, phoneNumber: String?, actionFinished: () -> Unit = { }) {
            val cleared = PhoneNumberUtils.clearFormatting(phoneNumber)
            val formatted = PhoneNumberUtils.format(cleared)

            if (cleared.isEmpty()) {
                AlertDialog.Builder(fragmentActivity)
                        .setMessage(R.string.blacklist_need_number)
                        .setPositiveButton(android.R.string.ok) { _, _ -> }
                        .show()
            } else {
                val message = fragmentActivity.getString(R.string.add_blacklist, formatted)
                AlertDialog.Builder(fragmentActivity)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val blacklist = Blacklist()
                            blacklist.phoneNumber = cleared
                            DataSource.insertBlacklist(fragmentActivity, blacklist)

                            actionFinished()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
            }
        }

        fun addBlacklistPhrase(fragmentActivity: Activity, phrase: String, actionFinished: () -> Unit = { }) {
            if (phrase.isBlank()) {
                AlertDialog.Builder(fragmentActivity)
                        .setMessage(R.string.blacklist_need_phrase)
                        .setPositiveButton(android.R.string.ok) { _, _ -> }
                        .show()
            } else {
                val message = fragmentActivity.getString(R.string.add_blacklist_phrase, phrase)
                AlertDialog.Builder(fragmentActivity)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val blacklist = Blacklist()
                            blacklist.phrase = phrase
                            DataSource.insertBlacklist(fragmentActivity, blacklist)

                            actionFinished()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
            }
        }
    }

}
