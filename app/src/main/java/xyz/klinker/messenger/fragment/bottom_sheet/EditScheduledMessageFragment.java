package xyz.klinker.messenger.fragment.bottom_sheet;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.IllegalFormatConversionException;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.ScheduledMessage;
import xyz.klinker.messenger.fragment.ScheduledMessagesFragment;


public class EditScheduledMessageFragment extends TabletOptimizedBottomSheetDialogFragment {

    private ScheduledMessagesFragment fragment;
    private ScheduledMessage scheduledMessage;

    private DateFormat format;

    private TextView sendDate;
    private EditText messageText;

    @Override
    protected View createLayout(LayoutInflater inflater) {
        final View contentView = inflater.inflate(R.layout.bottom_sheet_edit_scheduled_message, null, false);

        format = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT,
                SimpleDateFormat.SHORT);

        sendDate = (TextView) contentView.findViewById(R.id.send_time);
        messageText = (EditText) contentView.findViewById(R.id.message);
        TextView name = (TextView) contentView.findViewById(R.id.contact_name);
        Button delete = (Button) contentView.findViewById(R.id.delete);
        Button save = (Button) contentView.findViewById(R.id.save);

        messageText.setText(scheduledMessage.data);
        sendDate.setText(format.format(scheduledMessage.timestamp));
        name.setText(scheduledMessage.title);
        save.setOnClickListener(view -> save());
        delete.setOnClickListener(view -> delete());
        sendDate.setOnClickListener(view -> displayDateDialog());

        messageText.setSelection(messageText.getText().length());

        return contentView;
    }

    public void setMessage(ScheduledMessage message) {
        this.scheduledMessage = message;
    }

    public void setFragment(ScheduledMessagesFragment fragment) {
        this.fragment = fragment;
    }

    private void save() {
        scheduledMessage.data = messageText.getText().toString();
        DataSource.INSTANCE.updateScheduledMessage(getActivity(), scheduledMessage);

        dismiss();
        fragment.loadMessages();
    }

    private void delete() {
        DataSource.INSTANCE.deleteScheduledMessage(getActivity(), scheduledMessage.id);

        dismiss();
        fragment.loadMessages();
    }

    private void displayDateDialog() {
        Context context = getContextToFixDatePickerCrash();

        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(context, (datePicker, year, month, day) -> {
            scheduledMessage.timestamp = new GregorianCalendar(year, month, day)
                    .getTimeInMillis();
            displayTimeDialog();
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void displayTimeDialog() {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(getActivity(), (timePicker, hourOfDay, minute) -> {
            scheduledMessage.timestamp += (1000 * 60 * 60 * hourOfDay);
            scheduledMessage.timestamp += (1000 * 60 * minute);

            if (scheduledMessage.timestamp < System.currentTimeMillis()) {
                Toast.makeText(getActivity(), R.string.scheduled_message_in_future,
                        Toast.LENGTH_SHORT).show();
                displayDateDialog();
            } else {
                sendDate.setText(format.format(scheduledMessage.timestamp));
            }
        },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                android.text.format.DateFormat.is24HourFormat(getActivity()))
                .show();
    }

    // samsung messed up the date picker in some languages on Lollipop 5.0 and 5.1. Ugh.
    // fixes this issue: http://stackoverflow.com/a/34853067
    private ContextWrapper getContextToFixDatePickerCrash() {
        return new ContextWrapper(getActivity()) {

            private Resources wrappedResources;

            @Override
            public Resources getResources() {
                Resources r = super.getResources();
                if(wrappedResources == null) {
                    wrappedResources = new Resources(r.getAssets(), r.getDisplayMetrics(), r.getConfiguration()) {
                        @NonNull
                        @Override
                        public String getString(int id, Object... formatArgs) throws NotFoundException {
                            try {
                                return super.getString(id, formatArgs);
                            } catch (IllegalFormatConversionException ifce) {
                                Log.e("DatePickerDialogFix", "IllegalFormatConversionException Fixed!", ifce);
                                String template = super.getString(id);
                                template = template.replaceAll("%" + ifce.getConversion(), "%s");
                                return String.format(getConfiguration().locale, template, formatArgs);
                            }
                        }
                    };
                }
                return wrappedResources;
            }
        };
    }
}