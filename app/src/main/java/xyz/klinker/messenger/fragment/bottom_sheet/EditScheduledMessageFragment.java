package xyz.klinker.messenger.fragment.bottom_sheet;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Random;

import xyz.klinker.android.article.ArticleIntent;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.ScheduledMessage;
import xyz.klinker.messenger.util.TimeUtils;

import static android.content.Context.CLIPBOARD_SERVICE;

public class EditScheduledMessageFragment extends BottomSheetDialogFragment {

    private ScheduledMessage scheduledMessage;

    private TextView sendDate;
    private EditText messageText;

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) { }
        @Override public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }
    };

    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        final View contentView = View.inflate(getContext(), R.layout.bottom_sheet_edit_scheduled_message, null);
        dialog.setContentView(contentView);

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();

        if (behavior != null && behavior instanceof BottomSheetBehavior ) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }

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