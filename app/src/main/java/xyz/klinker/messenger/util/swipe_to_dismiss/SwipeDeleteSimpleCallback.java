package xyz.klinker.messenger.util.swipe_to_dismiss;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.data.Settings;

public class SwipeDeleteSimpleCallback extends SwipeSimpleCallback {

    public SwipeDeleteSimpleCallback(ConversationListAdapter adapter) {
        super(adapter);
    }

    @Override
    protected void setupEndSwipe(Context context) {
        Settings settings = Settings.get(context);
        if (settings.useGlobalThemeColor) {
            endSwipeBackground = new ColorDrawable(settings.globalColorSet.colorAccent);
        } else {
            endSwipeBackground = new ColorDrawable(context.getResources().getColor(R.color.colorAccent));
        }
        endMark = context.getDrawable(R.drawable.ic_delete_sweep);
        endMark.setColorFilter(context.getResources().getColor(R.color.deleteIcon), PorterDuff.Mode.SRC_ATOP);
    }
}
