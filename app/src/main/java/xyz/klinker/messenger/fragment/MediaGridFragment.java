package xyz.klinker.messenger.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.ImageViewerActivity;
import xyz.klinker.messenger.adapter.MediaGridAdapter;
import xyz.klinker.messenger.shared.activity.AbstractSettingsActivity;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.util.listener.MediaSelectedListener;

public class MediaGridFragment extends Fragment implements MediaSelectedListener {

    private static final String ARG_CONVERSATION_ID = "conversation_id";

    public Conversation conversation;
    private List<Message> messages;

    public static MediaGridFragment newInstance(long conversationId) {
        MediaGridFragment fragment = new MediaGridFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CONVERSATION_ID, conversationId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadConversation();
        setUpToolbar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_media_grid, container, false);
        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);

        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(),
                getResources().getInteger(R.integer.images_column_count)));
        recyclerView.setAdapter(new MediaGridAdapter(messages, this));

        return root;
    }

    private void loadConversation() {
        DataSource source = getDataSource();
        conversation = source.getConversation(getActivity(), getArguments().getLong(ARG_CONVERSATION_ID));
        messages = source.getMediaMessages(getActivity(), conversation.id);
    }

    @VisibleForTesting
    DataSource getDataSource() {
        return DataSource.INSTANCE;
    }

    private void setUpToolbar() {
        getActivity().setTitle(conversation.title);

        Settings settings = Settings.get(getActivity());
        Toolbar toolbar = ((AbstractSettingsActivity) getActivity()).getToolbar();

        if (toolbar != null) {
            if (settings.useGlobalThemeColor) {
                toolbar.setBackgroundColor(settings.mainColorSet.color);
                ActivityUtils.setStatusBarColor(getActivity(), settings.mainColorSet.colorDark);
            } else {
                toolbar.setBackgroundColor(conversation.colors.color);
                ActivityUtils.setStatusBarColor(getActivity(), conversation.colors.colorDark);
            }
        }
    }


    @Override
    public void onSelected(List<Message> messageList, int selectedPosition) {
        Intent intent = new Intent(getActivity(), ImageViewerActivity.class);
        intent.putExtra(ImageViewerActivity.EXTRA_CONVERSATION_ID, getArguments().getLong(ARG_CONVERSATION_ID));
        intent.putExtra(ImageViewerActivity.EXTRA_MESSAGE_ID, messageList.get(selectedPosition).id);
        startActivity(intent);
    }
}
