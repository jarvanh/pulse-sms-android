package xyz.klinker.messenger.fragment;

import android.database.Cursor;
import android.support.design.widget.NavigationView;
import android.support.v7.widget.helper.ItemTouchHelper;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.util.ActivityUtils;
import xyz.klinker.messenger.util.AnimationUtils;
import xyz.klinker.messenger.util.ColorUtils;
import xyz.klinker.messenger.util.swipe_to_dismiss.SwipeTouchHelper;
import xyz.klinker.messenger.util.swipe_to_dismiss.UnarchiveSwipeSimpleCallback;

public class ArchivedConversationListFragment extends ConversationListFragment {

    // only grab the archived messages
    @Override
    protected Cursor getCursor(DataSource source) {
        return source.getArchivedConversations();
    }

    // create swipe helper that has the unarchive icon instead of the archive one
    @Override
    public ItemTouchHelper getSwipeTouchHelper(ConversationListAdapter adapter) {
        return new SwipeTouchHelper(new UnarchiveSwipeSimpleCallback(adapter));
    }

    // change the text to "1 conversation moved to the inbox
    @Override
    protected String getArchiveSnackbarText() {
        return getResources().getQuantityString(R.plurals.conversations_unarchived,
                pendingArchive.size(), pendingArchive.size());
    }

    // unarchive instead of archive when the snackbar is dismissed
    @Override
    protected void performArchiveOperation(DataSource dataSource, Conversation conversation) {
        dataSource.unarchiveConversation(conversation.id);
    }

    // always consume the back event and send us to the conversation list
    @Override
    public boolean onBackPressed() {
        if (!super.onBackPressed()) {
            NavigationView navView = (NavigationView) getActivity().findViewById(R.id.navigation_view);
            navView.getMenu().getItem(0).setChecked(true);

            ((MessengerActivity) getActivity()).displayConversations();
        }

        return true;
    }
    
    @Override
    public void onConversationContracted(ConversationViewHolder viewHolder) {
        super.onConversationContracted(viewHolder);

        NavigationView navView = (NavigationView) getActivity().findViewById(R.id.navigation_view);
        navView.getMenu().getItem(1).setChecked(true);
    }

}
