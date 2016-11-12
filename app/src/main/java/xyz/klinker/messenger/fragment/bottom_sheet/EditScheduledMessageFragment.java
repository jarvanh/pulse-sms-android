package xyz.klinker.messenger.fragment.bottom_sheet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.ScheduledMessage;


public class EditScheduledMessageFragment extends TabletOptimizedBottomSheetDialogFragment {

    private ScheduledMessage scheduledMessage;

    private TextView sendDate;
    private EditText messageText;

    @Override
    protected View createLayout(LayoutInflater inflater) {
        final View contentView = inflater.inflate(R.layout.bottom_sheet_edit_scheduled_message, null, false);

        DateFormat format = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT,
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
        sendDate.setOnClickListener(view -> editDate());

        return contentView;
    }

    public void setMessage(ScheduledMessage message) {
        this.scheduledMessage = message;
    }

    private void save() {
        scheduledMessage.data = messageText.getText().toString();
        scheduledMessage.timestamp = scheduledMessage.timestamp; // todo

        DataSource source = DataSource.getInstance(getActivity());
        source.open();
        // source.updateScheduledMessage(scheduledMessage);
        source.close();

        dismiss();
    }

    private void delete() {
        DataSource source = DataSource.getInstance(getActivity());
        source.open();
        source.deleteScheduledMessage(scheduledMessage.id);
        source.close();

        dismiss();
    }

    private void editDate() {

    }
}