package xyz.klinker.messenger.util.swipe_to_dismiss;

import android.content.Context;
import android.graphics.drawable.Drawable;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ConversationListAdapter;

public class UnarchiveSwipeSimpleCallback extends SwipeSimpleCallback {

    public UnarchiveSwipeSimpleCallback(ConversationListAdapter adapter) {
        super(adapter);
    }

    @Override
    protected Drawable getArchiveItem(Context context) {
        return context.getDrawable(R.drawable.ic_unarchive);
    }
}
