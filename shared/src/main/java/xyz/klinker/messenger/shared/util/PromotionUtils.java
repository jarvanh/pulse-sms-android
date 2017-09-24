package xyz.klinker.messenger.shared.util;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.activity.RateItDialog;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.Settings;

public class PromotionUtils {

    private Context context;
    private SharedPreferences sharedPreferences;

    public PromotionUtils(Context context) {
        this.context = context;
        this.sharedPreferences = Settings.get(context).getSharedPrefs(context);
    }

    public void checkPromotions() {
        if (shouldAskForRating()) {
            askForRating();
        }
    }

    private boolean shouldAskForRating() {
        String pref = "install_time";
        long currentTime = System.currentTimeMillis();
        long installTime = sharedPreferences.getLong(pref, -1L);

        if (installTime == -1L) {
            // write the install time to now
            sharedPreferences.edit().putLong(pref, currentTime).apply();
        } else {
            if (currentTime - installTime > TimeUtils.TWO_WEEKS) {
                return sharedPreferences.getBoolean("show_rate_it", true);
            }
        }

        return false;
    }

    private void askForRating() {
        sharedPreferences.edit().putBoolean("show_rate_it", false)
                .apply();

        new Handler().postDelayed(() ->
                context.startActivity(new Intent(context, RateItDialog.class)),
                1000);
    }
}
