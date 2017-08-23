package xyz.klinker.messenger.shared.util;

import android.database.Cursor;

public class CursorUtil {

    public static void closeSilent(Cursor cursor) {
        try {
            cursor.close();
        } catch (Exception e) { }
    }
}
