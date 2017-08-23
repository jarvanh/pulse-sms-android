package xyz.klinker.messenger.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.media.MediaRecorder;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.AttachContactAdapter;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.StringUtils;
import xyz.klinker.messenger.shared.util.listener.AttachContactListener;
import xyz.klinker.messenger.shared.util.listener.AudioRecordedListener;

/**
 * View that allows you to select a contact and attach it to a message.
 */
@SuppressLint("ViewConstructor")
public class AttachContactView extends FrameLayout {

    private static final String TAG = "RecordAudioView";

    private AttachContactListener listener;

    public AttachContactView(Context context, AttachContactListener listener, int color) {
        super(context);

        this.listener = listener;
        init(color);
    }

    private void init(int color) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.view_attach_contact, this, true);

        final RecipientEditTextView contactEntry =
                (RecipientEditTextView) findViewById(R.id.contact_entry);
        contactEntry.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        BaseRecipientAdapter baseRecipientAdapter = new BaseRecipientAdapter(
                BaseRecipientAdapter.QUERY_TYPE_PHONE, getContext());

        baseRecipientAdapter.setShowMobileOnly(false);
        contactEntry.setAdapter(baseRecipientAdapter);
        contactEntry.setPostSelectedAction(() -> {
            DrawableRecipientChip[] chips = contactEntry.getSortedRecipients();
            if (chips.length > 0) {
                String name = chips[0].getEntry().getDisplayName();
                String phone = chips[0].getEntry().getDestination();

                String firstName = "";
                String lastName = "";

                if (name.split(" ").length > 1) {
                    firstName = name.split(" ")[0];
                    lastName = name.split(" ")[1];
                }

                listener.onContactAttached(firstName, lastName, phone);
            }
        });

        final RecyclerView recentsList = (RecyclerView) findViewById(R.id.recycler_view);
        ColorUtils.changeRecyclerOverscrollColors(recentsList, color);
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recentsList.setLayoutManager(layoutManager);

        final AttachContactAdapter adapter = new AttachContactAdapter(listener);
        recentsList.setAdapter(adapter);

        final Handler ui = new Handler();
        new Thread(() -> {
            DataSource source = DataSource.Companion.getInstance(getContext());
            source.open();
            List<Conversation> conversations = source.getUnarchivedConversationsAsList();
            source.close();

            ui.post(() -> adapter.setContacts(conversations));
        }).start();
    }
}
