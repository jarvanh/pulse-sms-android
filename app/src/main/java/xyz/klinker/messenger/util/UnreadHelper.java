package xyz.klinker.messenger.util;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import me.leolin.shortcutbadger.ShortcutBadger;
import xyz.klinker.messenger.activity.MessengerActivity;

public class UnreadHelper {

    private Context context;

    public UnreadHelper(Context context) {
        this.context = context;
    }

    public void writeCounts(final int newCount) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                shortcutBadger(newCount);
                //teslaUnread(newCount);
                //samsung(newCount);
            }
        }).start();
    }

    private void teslaUnread(final int count) {

    }

    private void shortcutBadger(final int count) {
        ShortcutBadger.applyCount(context, count);
    }

    private void samsung(final int count) {
        ContentValues cv = new ContentValues();
        cv.put("package", context.getPackageName());
        cv.put("class", context.getPackageName() + ".activity.MessengerActivity");
        cv.put("badgecount", count); // integer count you want to display

        try {
            context.getContentResolver().insert(Uri.parse("content://com.sec.badge/apps"), cv);
        } catch (Exception e) { }
    }
}
