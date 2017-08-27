package xyz.klinker.messenger.shared.util;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import me.leolin.shortcutbadger.ShortcutBadger;
import xyz.klinker.messenger.shared.data.DataSource;

public class UnreadBadger {

    private Context context;

    public UnreadBadger(Context context) {
        this.context = context;
    }

    public void writeCountFromDatabase() {
        if (context == null) {
            return;
        }

        try {
            int count = DataSource.INSTANCE.getUnreadConversationCount(context);
            writeCount(count);
        } catch (Exception e) {

        }
    }

    public void writeCount(final int newCount) {
        new Thread(() -> shortcutBadger(newCount)).start();
    }

    public void clearCount() {
        writeCount(0);
    }

    private void shortcutBadger(final int count) {
        if (context != null) {
            try {
                ShortcutBadger.applyCountOrThrow(context, count);
            } catch (Exception e) { }
        }
    }

    // region handled by shortcut badger
    private void teslaUnread(final int count) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("tag", context.getPackageName() + "/" + context.getPackageName() + ".activity.MessengerActivity");
            cv.put("count", count);

            context.getContentResolver().insert(
                    Uri.parse("content://com.teslacoilsw.notifier/unread_count"),
                    cv);
        } catch (Exception ex) { }
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
    //endregion
}
