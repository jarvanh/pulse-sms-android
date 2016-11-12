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

import java.util.Random;

import xyz.klinker.android.article.ArticleIntent;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.ScheduledMessage;

import static android.content.Context.CLIPBOARD_SERVICE;

public class EditScheduledMessageFragment extends BottomSheetDialogFragment {

    private ScheduledMessage message;

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
        final View contentView = View.inflate(getContext(), R.layout.bottom_sheet_link, null);
        dialog.setContentView(contentView);

        TextView name = (TextView) contentView.findViewById(R.id.contact_name);
        TextView sendDate = (TextView) contentView.findViewById(R.id.send_time);
        EditText message = (EditText) contentView.findViewById(R.id.message);
        Button delete = (Button) contentView.findViewById(R.id.delete);
        Button save = (Button) contentView.findViewById(R.id.save);

        
    }

    public void setMessage(ScheduledMessage message) {
        this.message = message;
    }
}