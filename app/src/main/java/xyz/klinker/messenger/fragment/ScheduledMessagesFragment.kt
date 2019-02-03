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
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.*
import android.content.res.ColorStateList
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.ex.chips.BaseRecipientAdapter
import com.android.ex.chips.RecipientEditTextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import xyz.klinker.giphy.Giphy
import xyz.klinker.messenger.BuildConfig
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.compose.ShareData
import xyz.klinker.messenger.adapter.ScheduledMessagesAdapter
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.fragment.bottom_sheet.EditScheduledMessageFragment
import xyz.klinker.messenger.fragment.message.attach.AttachmentListener
import xyz.klinker.messenger.shared.data.*
import xyz.klinker.messenger.shared.data.model.ScheduledMessage
import xyz.klinker.messenger.shared.service.jobs.ScheduledMessageJob
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.ImageUtils
import xyz.klinker.messenger.shared.util.PhoneNumberUtils
import xyz.klinker.messenger.shared.util.TimeUtils
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

    private var imageData: ShareData? = null
    private var messageInProcess: ScheduledMessage? = null

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

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_schedule_messages, parent, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.layoutManager = LinearLayoutManager(fragmentActivity)
        fab.setOnClickListener { startSchedulingMessage() }

        emptyView.setBackgroundColor(Settings.mainColorSet.colorLight)
        fab.backgroundTintList = ColorStateList.valueOf(Settings.mainColorSet.colorAccent)
        ColorUtils.changeRecyclerOverscrollColors(list, Settings.mainColorSet.color)

        loadMessages()

        val arguments = arguments
        if (arguments?.getString(ARG_TITLE) != null && arguments.getString(ARG_PHONE_NUMBERS) != null) {
            val message = ScheduledMessage()
            message.to = arguments.getString(ARG_PHONE_NUMBERS)
            message.title = arguments.getString(ARG_TITLE)
            message.data = arguments.getString(ARG_DATA)
            message.mimeType = MimeType.TEXT_PLAIN

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AttachmentListener.RESULT_GALLERY_PICKER_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.data != null) {
            val uri = data.data!!
            val mimeType = MimeType.IMAGE_JPEG
            val pulseUri = ImageUtils.scaleToSend(fragmentActivity!!, uri, mimeType)

            if (pulseUri != null) {
                imageData = ShareData(mimeType, pulseUri.toString())
            }

            if (messageInProcess != null) {
                displayMessageDialog(messageInProcess!!)
            }
        } else if (requestCode == Giphy.REQUEST_GIPHY && resultCode == Activity.RESULT_OK
                && data != null && data.data != null) {
            val uriString = data.data!!.toString()
            val mimeType = MimeType.IMAGE_GIF
            imageData = ShareData(mimeType, uriString)

            if (messageInProcess != null) {
                displayMessageDialog(messageInProcess!!)
            }
        }

        messageInProcess = null
    }

    fun loadMessages() {
        Thread {
            try {
                if (fragmentActivity != null) {
                    val messages = DataSource.getScheduledMessagesAsList(fragmentActivity!!)
                    fragmentActivity!!.runOnUiThread { setMessages(messages) }
                }
            } catch (e: Exception) {
            }
        }.start()
    }

    private fun setMessages(messages: List<ScheduledMessage>) {
        progress?.visibility = View.GONE
        list.adapter = ScheduledMessagesAdapter(messages, this)

        if (list.adapter!!.itemCount == 0) {
            emptyView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.GONE
        }
    }

    override fun onClick(message: ScheduledMessage) {
        if (message.mimeType != MimeType.TEXT_PLAIN && fragmentActivity != null) {
            AlertDialog.Builder(fragmentActivity!!)
                    .setMessage(R.string.remove_scheduled_message)
                    .setPositiveButton(R.string.api_yes) { _, _ ->
                        DataSource.deleteScheduledMessage(fragmentActivity!!, message.id)
                        loadMessages()
                    }.setNegativeButton(R.string.api_no) { _, _ -> }
                    .show()
        } else {
            val fragment = EditScheduledMessageFragment()
            fragment.setMessage(message)
            fragment.setFragment(this)
            fragment.show(fragmentActivity?.supportFragmentManager, "")
        }
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

        editText.post {
            editText.requestFocus()
            (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        }

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

            if (message.timestamp > TimeUtils.now) {
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

        val layout = LayoutInflater.from(fragmentActivity).inflate(R.layout.dialog_scheduled_message_content, null, false)
        val editText = layout.findViewById<EditText>(R.id.edit_text)
        val repeat = layout.findViewById<Spinner>(R.id.repeat_interval)
        val image = layout.findViewById<ImageView>(R.id.image)

        repeat.adapter = ArrayAdapter.createFromResource(fragmentActivity!!, R.array.scheduled_message_repeat, android.R.layout.simple_spinner_dropdown_item)

        if (imageData != null) {
            image.visibility = View.VISIBLE
            if (imageData!!.mimeType == MimeType.IMAGE_GIF) {
                Glide.with(fragmentActivity!!)
                        .asGif()
                        .load(imageData!!.data)
                        .into(image)
            } else {
                Glide.with(fragmentActivity!!)
                        .load(imageData!!.data)
                        .apply(RequestOptions().centerCrop())
                        .into(image)
            }
        }

        if (message.data != null && message.data!!.isNotEmpty()) {
            editText.setText(message.data)
            editText.setSelection(message.data!!.length)
        }

        editText.post {
            editText.requestFocus()
            (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        }

        val builder = AlertDialog.Builder(fragmentActivity!!)
                .setView(layout)
                .setPositiveButton(R.string.save) { _, _ ->
                    if (editText.text.isNotEmpty() || image != null) {
                        message.repeat = repeat.selectedItemPosition

                        val messages = mutableListOf<ScheduledMessage>()

                        if (editText.text.isNotEmpty()) {
                            messages.add(ScheduledMessage().apply {
                                this.id = DataSource.generateId()
                                this.repeat = message.repeat
                                this.timestamp = message.timestamp
                                this.title = message.title
                                this.to = message.to
                                this.data = editText.text.toString()
                                this.mimeType = MimeType.TEXT_PLAIN
                            })
                        }

                        if (imageData != null) {
                            messages.add(ScheduledMessage().apply {
                                this.id = DataSource.generateId()
                                this.repeat = message.repeat
                                this.timestamp = message.timestamp
                                this.title = message.title
                                this.to = message.to
                                this.data = imageData!!.data
                                this.mimeType = imageData!!.mimeType
                            })
                        }

                        saveMessages(messages)
                        imageData = null

                        (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                                ?.hideSoftInputFromWindow(editText.windowToken, 0)
                    } else {
                        displayMessageDialog(message)
                    }
                }.setNegativeButton(android.R.string.cancel) { _, _ ->
                    imageData = null
                    (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                            ?.hideSoftInputFromWindow(editText.windowToken, 0)
                }

        if (!Account.exists() || Account.primary) {
            if (imageData == null) {
                builder.setNeutralButton(R.string.attach_image) { _, _ ->
                    if (editText.text.isNotEmpty()) {
                        message.data = editText.text.toString()
                    } else {
                        message.data = null
                    }

                    (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                            ?.hideSoftInputFromWindow(editText.windowToken, 0)

                    AlertDialog.Builder(fragmentActivity!!)
                            .setItems(R.array.scheduled_message_attachment_options) { _, position ->
                                messageInProcess = message

                                when (position) {
                                    0 -> {
                                        val intent = Intent()
                                        intent.type = "image/*"
                                        intent.action = Intent.ACTION_GET_CONTENT
                                        activity?.startActivityForResult(Intent.createChooser(intent, "Select Picture"), AttachmentListener.RESULT_GALLERY_PICKER_REQUEST)
                                    }
                                    1 -> {
                                        Giphy.Builder(activity, BuildConfig.GIPHY_API_KEY)
                                                .maxFileSize(MmsSettings.maxImageSize)
                                                .start()
                                    }
                                }
                            }.show()
                }
            } else {
                builder.setNeutralButton(R.string.remove_image_short) { _, _ ->
                    if (editText.text.isNotEmpty()) {
                        message.data = editText.text.toString()
                    } else {
                        message.data = null
                    }

                    imageData = null
                    displayMessageDialog(message)
                }
            }
        }

        builder.show()
    }

    private fun saveMessages(messages: List<ScheduledMessage>) {
        Thread {
            messages.forEach { DataSource.insertScheduledMessage(fragmentActivity!!, it) }
            loadMessages()
        }.start()
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

        private const val ARG_TITLE = "title"
        private const val ARG_PHONE_NUMBERS = "phone_numbers"
        private const val ARG_DATA = "data"

        fun newInstance(): ScheduledMessagesFragment {
            return ScheduledMessagesFragment()
        }

        fun newInstance(title: String, phoneNumbers: String, text: String): ScheduledMessagesFragment {
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_PHONE_NUMBERS, phoneNumbers)
            args.putString(ARG_DATA, text)

            val fragment = ScheduledMessagesFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
