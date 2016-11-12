package xyz.klinker.messenger.fragment.bottom_sheet;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.ComposeActivity;
import xyz.klinker.messenger.data.model.Message;

import static android.content.Context.CLIPBOARD_SERVICE;

public class MessageShareFragment extends TabletOptimizedBottomSheetDialogFragment {

    private Message message;

    @Override
    protected View createLayout(LayoutInflater inflater) {
        final View contentView = View.inflate(getContext(), R.layout.bottom_sheet_share, null);

        View shareExternal = contentView.findViewById(R.id.share_external);
        View forwardToContact = contentView.findViewById(R.id.forward_to_contact);
        View copyText = contentView.findViewById(R.id.copy_text);

        shareExternal.setOnClickListener(view -> {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, message.data);
            shareIntent.setType(message.mimeType);
            getActivity().startActivity(Intent.createChooser(shareIntent,
                    getActivity().getResources().getText(R.string.share_content)));

            dismiss();
        });

        forwardToContact.setOnClickListener(view -> {
            Intent shareIntent = new Intent(getActivity(), ComposeActivity.class);
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, message.data);
            shareIntent.setType(message.mimeType);
            getActivity().startActivity(shareIntent);

            dismiss();
        });

        copyText.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager)
                    getActivity().getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("messenger",
                    message.data.toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getActivity(), R.string.message_copied_to_clipboard,
                    Toast.LENGTH_SHORT).show();

            dismiss();
        });

        return contentView;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
