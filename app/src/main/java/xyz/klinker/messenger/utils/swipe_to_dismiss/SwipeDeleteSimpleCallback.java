package xyz.klinker.messenger.utils.swipe_to_dismiss;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.util.ColorUtils;

public class SwipeDeleteSimpleCallback extends SwipeSimpleCallback {

    public SwipeDeleteSimpleCallback(ConversationListAdapter adapter) {
        super(adapter);
    }

    @Override
    protected void setupEndSwipe(Context context) {
        endSwipeBackground = new ColorDrawable(Settings.get(context).mainColorSet.getColorAccent());
        endMark = context.getDrawable(R.drawable.ic_delete_sweep);

        if (ColorUtils.INSTANCE.isColorDark(Settings.get(context).mainColorSet.getColorAccent())) {
            endMark.setTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.deleteIcon)));
        } else {
            endMark.setTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.lightToolbarTextColor)));
        }
    }
}
