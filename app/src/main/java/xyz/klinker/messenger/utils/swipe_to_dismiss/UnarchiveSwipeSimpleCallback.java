package xyz.klinker.messenger.utils.swipe_to_dismiss;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.Settings;

public class UnarchiveSwipeSimpleCallback extends SwipeSimpleCallback {

    public UnarchiveSwipeSimpleCallback(ConversationListAdapter adapter) {
        super(adapter);
    }

    @Override
    protected Drawable getArchiveItem(Context context) {
        return context.getDrawable(R.drawable.ic_unarchive);
    }

    @Override
    protected void setupEndSwipe(Context context) {
        endSwipeBackground = new ColorDrawable(Settings.INSTANCE.getMainColorSet().getColorAccent());
        endMark = context.getDrawable(R.drawable.ic_delete_sweep);
        endMark.setColorFilter(context.getResources().getColor(R.color.deleteIcon), PorterDuff.Mode.SRC_ATOP);
    }
}
