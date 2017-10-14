package xyz.klinker.messenger.shared.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import java.util.List;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider;

// similar to:
// https://github.com/romannurik/dashclock/blob/master/main/src/main/java/com/google/android/apps/dashclock/phone/SmsExtension.java
public class DashclockExtension extends DashClockExtension {

    public BroadcastReceiver update = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onUpdateData(0);
        }
    };

    @Override
    public void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);

        try {
            unregisterReceiver(update);
        } catch (Exception e) {
            e.printStackTrace();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(MessengerAppWidgetProvider.Companion.getREFRESH_ACTION());
        registerReceiver(update, filter);
    }

    protected void onUpdateData(int reason) {
        List<Conversation> conversations = DataSource.INSTANCE.getUnreadConversationsAsList(this);
        publishUpdate(new ExtensionData()
                .visible(conversations.size() > 0)
                .icon(R.drawable.ic_stat_notify_group)
                .status(getStatus(conversations))
                .expandedTitle(getExpandedStatus(conversations))
                .expandedBody(getBody(conversations))
                .clickIntent(getIntent(conversations)));
    }

    private String getStatus(List<Conversation> conversations) {
        return getResources().getQuantityString(R.plurals.new_conversations,
                conversations.size(), conversations.size());
    }

    private String getExpandedStatus(List<Conversation> conversations) {
        if (conversations.size() == 1) {
            return conversations.get(0).getTitle();
        } else {
            return getResources().getQuantityString(R.plurals.new_conversations,
                    conversations.size(), conversations.size());
        }
    }

    private String getBody(List<Conversation> conversations) {
        if (conversations.size() == 1) {
            return conversations.get(0).getSnippet();
        } else if (conversations.size() > 1) {
            StringBuilder builder = new StringBuilder(conversations.get(0).getTitle());
            for (int i = 1; i < conversations.size(); i++) {
                builder.append(", ").append(conversations.get(i).getTitle());
            }
            return builder.toString();
        } else {
            return "";
        }
    }

    private Intent getIntent(List<Conversation> conversations) {
        final Intent intent = ActivityUtils.INSTANCE.buildForComponent(ActivityUtils.INSTANCE.getMESSENGER_ACTIVITY());

        if (conversations.size() == 1) {
            intent.setData(Uri.parse("https://messenger.klinkerapps.com/" + conversations.get(0).getId()));
        }

        return intent;
    }
}