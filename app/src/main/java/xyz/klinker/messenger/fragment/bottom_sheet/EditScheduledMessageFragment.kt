package xyz.klinker.messenger.fragment.bottom_sheet

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.IllegalFormatConversionException

import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.ScheduledMessage
import xyz.klinker.messenger.fragment.ScheduledMessagesFragment

@Suppress("DEPRECATION")
class EditScheduledMessageFragment : TabletOptimizedBottomSheetDialogFragment() {

    private var fragment: ScheduledMessagesFragment? = null
    private var scheduledMessage: ScheduledMessage? = null

    private var format: DateFormat? = null

    private var sendDate: TextView? = null
    private var messageText: EditText? = null

    // samsung messed up the date picker in some languages on Lollipop 5.0 and 5.1. Ugh.
    // fixes this issue: http://stackoverflow.com/a/34853067
    private val contextToFixDatePickerCrash: ContextWrapper
        get() = object : ContextWrapper(activity) {

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

    override fun createLayout(inflater: LayoutInflater): View {
        val contentView = inflater.inflate(R.layout.bottom_sheet_edit_scheduled_message, null, false)

        format = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT,
                SimpleDateFormat.SHORT)

        sendDate = contentView.findViewById<View>(R.id.send_time) as TextView
        messageText = contentView.findViewById<View>(R.id.message) as EditText
        val name = contentView.findViewById<View>(R.id.contact_name) as TextView
        val delete = contentView.findViewById<View>(R.id.delete) as Button
        val save = contentView.findViewById<View>(R.id.save) as Button

        if (scheduledMessage != null) {
            messageText?.setText(scheduledMessage!!.data)
            sendDate?.text = format!!.format(scheduledMessage!!.timestamp)
            name.text = scheduledMessage!!.title
            messageText?.setSelection(messageText!!.text.length)
        }

        save.setOnClickListener { save() }
        delete.setOnClickListener { delete() }
        sendDate?.setOnClickListener { displayDateDialog() }

        return contentView
    }

    fun setMessage(message: ScheduledMessage) {
        this.scheduledMessage = message
    }

    fun setFragment(fragment: ScheduledMessagesFragment) {
        this.fragment = fragment
    }

    private fun save() {
        val activity = activity ?: return

        if (scheduledMessage == null || messageText == null) {
            return
        }

        scheduledMessage!!.data = messageText!!.text.toString()
        DataSource.updateScheduledMessage(activity, scheduledMessage!!)

        dismiss()
        fragment?.loadMessages()
    }

    private fun delete() {
        val activity = activity ?: return

        DataSource.deleteScheduledMessage(activity, scheduledMessage!!.id)

        dismiss()
        fragment?.loadMessages()
    }

    private fun displayDateDialog() {
        val context = contextToFixDatePickerCrash

        val calendar = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            scheduledMessage!!.timestamp = GregorianCalendar(year, month, day)
                    .timeInMillis
            displayTimeDialog()
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show()
    }

    private fun displayTimeDialog() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(activity, { _, hourOfDay, minute ->
            scheduledMessage!!.timestamp += (1000 * 60 * 60 * hourOfDay).toLong()
            scheduledMessage!!.timestamp += (1000 * 60 * minute).toLong()

            if (scheduledMessage!!.timestamp < System.currentTimeMillis()) {
                Toast.makeText(activity, R.string.scheduled_message_in_future,
                        Toast.LENGTH_SHORT).show()
                displayDateDialog()
            } else {
                sendDate!!.text = format!!.format(scheduledMessage!!.timestamp)
            }
        },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                android.text.format.DateFormat.is24HourFormat(activity))
                .show()
    }
}