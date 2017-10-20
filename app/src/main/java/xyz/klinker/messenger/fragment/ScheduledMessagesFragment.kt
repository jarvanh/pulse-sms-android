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

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.*
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.MultiAutoCompleteTextView
import android.widget.ProgressBar
import android.widget.Toast
import com.android.ex.chips.BaseRecipientAdapter
import com.android.ex.chips.RecipientEditTextView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.ScheduledMessagesAdapter
import xyz.klinker.messenger.fragment.bottom_sheet.EditScheduledMessageFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.ScheduledMessage
import xyz.klinker.messenger.shared.service.jobs.ScheduledMessageJob
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.PhoneNumberUtils
import xyz.klinker.messenger.shared.util.listener.ScheduledMessageClickListener
import java.util.*

@Suppress("DEPRECATION")
/**
 * Fragment for displaying scheduled messages.
 */
class ScheduledMessagesFragment : Fragment(), ScheduledMessageClickListener {

    private val fragmentActivity: FragmentActivity? by lazy { activity }

    private val list: RecyclerView by lazy { view!!.findViewById<View>(R.id.list) as RecyclerView }
    private val progress: ProgressBar? by lazy { view?.findViewById<View>(R.id.progress) as ProgressBar? }
    private val fab: FloatingActionButton by lazy { view!!.findViewById<View>(R.id.fab) as FloatingActionButton }
    private val emptyView: View by lazy { view!!.findViewById<View>(R.id.empty_view) }

    private val scheduledMessageSent = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            loadMessages()
        }
    }

    // samsung messed up the date picker in some languages on Lollipop 5.0 and 5.1. Ugh.
    // fixes this issue: http://stackoverflow.com/a/34853067
    private val contextToFixDatePickerCrash: ContextWrapper
        get() = object : ContextWrapper(fragmentActivity!!) {
            private var wrappedResources: Resources? = null
            override fun getResources(): Resources {
                val r = super.getResources()
                if (wrappedResources == null) {
                    wrappedResources = object : Resources(r.assets, r.displayMetrics, r.configuration) {
                        @Throws(Resources.NotFoundException::class)
                        override fun getString(id: Int, vararg formatArgs: Any): String {
                            return try {
                                super.getString(id, *formatArgs)
                            } catch (ifce: IllegalFormatConversionException) {
                                Log.e("DatePickerDialogFix", "IllegalFormatConversionException Fixed!", ifce)
                                var template = super.getString(id)
                                template = template.replace(("%" + ifce.conversion).toRegex(), "%s")
                                String.format(configuration.locale, template, *formatArgs)
                            }

                        }
                    }
                }

                return wrappedResources!!
            }
        }

    override fun onCreateView(inflater: LayoutInflater?, parent: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater!!.inflate(R.layout.fragment_schedule_messages, parent, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.layoutManager = LinearLayoutManager(fragmentActivity)
        fab.setOnClickListener { startSchedulingMessage() }

        emptyView.setBackgroundColor(Settings.mainColorSet.colorLight)
        fab.backgroundTintList = ColorStateList.valueOf(Settings.mainColorSet.colorAccent)
        ColorUtils.changeRecyclerOverscrollColors(list, Settings.mainColorSet.color)

        loadMessages()

        if (arguments != null && arguments.getString(ARG_TITLE) != null &&
                arguments.getString(ARG_PHONE_NUMBERS) != null) {
            val message = ScheduledMessage()
            message.to = arguments.getString(ARG_PHONE_NUMBERS)
            message.title = arguments.getString(ARG_TITLE)
            displayDateDialog(message)
        }
    }

    override fun onStart() {
        super.onStart()
        fragmentActivity?.registerReceiver(scheduledMessageSent,
                IntentFilter(ScheduledMessageJob.BROADCAST_SCHEDULED_SENT))
    }

    override fun onStop() {
        super.onStop()

        try {
            fragmentActivity?.unregisterReceiver(scheduledMessageSent)
        } catch (e: Exception) {
        }

        ScheduledMessageJob.scheduleNextRun(fragmentActivity!!)
    }

    fun loadMessages() {
        val handler = Handler()
        Thread {
            if (fragmentActivity != null) {
                val messages = DataSource.getScheduledMessagesAsList(fragmentActivity!!)
                handler.post { setMessages(messages) }
            }
        }.start()
    }

    private fun setMessages(messages: List<ScheduledMessage>) {
        progress?.visibility = View.GONE
        list.adapter = ScheduledMessagesAdapter(messages, this)

        if (list.adapter.itemCount == 0) {
            emptyView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.GONE
        }
    }

    override fun onClick(message: ScheduledMessage) {
        val fragment = EditScheduledMessageFragment()
        fragment.setMessage(message)
        fragment.setFragment(this)
        fragment.show(fragmentActivity?.supportFragmentManager, "")
    }

    private fun startSchedulingMessage() {
        val message = ScheduledMessage()
        displayNameDialog(message)
    }

    private fun displayNameDialog(message: ScheduledMessage) {
        val layout = LayoutInflater.from(fragmentActivity)
                .inflate(R.layout.dialog_recipient_edit_text, null, false)
        val editText = layout.findViewById<View>(R.id.edit_text) as RecipientEditTextView
        editText.setHint(R.string.scheduled_to_hint)
        editText.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        val adapter = BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, fragmentActivity!!)
        adapter.isShowMobileOnly = Settings.mobileOnly
        editText.setAdapter(adapter)

        val dialog = AlertDialog.Builder(fragmentActivity!!)
                .setView(layout)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    when {
                        editText.recipients.isNotEmpty() -> {
                            val to = StringBuilder()
                            val title = StringBuilder()

                            for (chip in editText.recipients) {
                                to.append(PhoneNumberUtils.clearFormatting(chip.entry.destination))
                                title.append(chip.entry.displayName)
                                to.append(", ")
                                title.append(", ")
                            }

                            message.to = to.toString()
                            message.title = title.toString()

                            message.to = message.to!!.substring(0, message.to!!.length - 2)
                            message.title = message.title!!.substring(0, message.title!!.length - 2)
                        }
                        editText.text.isNotEmpty() -> {
                            message.to = PhoneNumberUtils.clearFormatting(editText
                                    .text.toString())
                            message.title = message.to
                        }
                        else -> {
                            displayNameDialog(message)
                            return@setPositiveButton
                        }
                    }

                    dismissKeyboard(editText)
                    displayDateDialog(message)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun displayDateDialog(message: ScheduledMessage) {
        var context: Context? = contextToFixDatePickerCrash

        if (context == null) {
            context = fragmentActivity
        }

        if (context == null) {
            return
        }

        val calendar = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            message.timestamp = GregorianCalendar(year, month, day)
                    .timeInMillis
            displayTimeDialog(message)
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show()
    }

    private fun displayTimeDialog(message: ScheduledMessage) {
        if (fragmentActivity == null) {
            return
        }

        val calendar = Calendar.getInstance()
        TimePickerDialog(fragmentActivity, { _, hourOfDay, minute ->
            message.timestamp = message.timestamp + 1000 * 60 * 60 * hourOfDay
            message.timestamp = message.timestamp + 1000 * 60 * minute

            if (message.timestamp > System.currentTimeMillis()) {
                displayMessageDialog(message)
            } else {
                Toast.makeText(fragmentActivity, R.string.scheduled_message_in_future,
                        Toast.LENGTH_SHORT).show()
                displayDateDialog(message)
            }
        },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(fragmentActivity))
                .show()
    }

    private fun displayMessageDialog(message: ScheduledMessage) {

        val layout = LayoutInflater.from(fragmentActivity).inflate(R.layout.dialog_edit_text, null, false)
        val editText = layout.findViewById<View>(R.id.edit_text) as EditText
        editText.setHint(R.string.scheduled_message_hint)

        AlertDialog.Builder(fragmentActivity!!)
                .setView(layout)
                .setPositiveButton(R.string.add) { _, _ ->
                    if (editText.text.isNotEmpty()) {
                        message.data = editText.text.toString()
                        message.mimeType = MimeType.TEXT_PLAIN
                        saveMessage(message)
                    } else {
                        displayMessageDialog(message)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun saveMessage(message: ScheduledMessage) {
        DataSource.insertScheduledMessage(fragmentActivity!!, message)
        loadMessages()
    }

    private fun dismissKeyboard(editText: EditText?) {
        if (editText == null) {
            return
        }

        try {
            val imm = fragmentActivity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editText.windowToken, 0)
        } catch (e: Exception) {
        }
    }

    companion object {

        private val ARG_TITLE = "title"
        private val ARG_PHONE_NUMBERS = "phone_numbers"

        fun newInstance(): ScheduledMessagesFragment {
            return ScheduledMessagesFragment()
        }

        fun newInstance(title: String, phoneNumbers: String): ScheduledMessagesFragment {
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_PHONE_NUMBERS, phoneNumbers)

            val fragment = ScheduledMessagesFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
