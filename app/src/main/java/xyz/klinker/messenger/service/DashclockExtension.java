package xyz.klinker.messenger.service;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.Conversation;

// similar to:
// https://github.com/romannurik/dashclock/blob/master/main/src/main/java/com/google/android/apps/dashclock/phone/SmsExtension.java
public class DashclockExtension extends DashClockExtension {

    @Override
    public void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        if (!isReconnect) {
            addWatchContentUris(new String[]{
                    "content://mms-sms/",
            });
        }
    }

    protected void onUpdateData(int reason) {

        DataSource source = DataSource.getInstance(this);
        source.open();
        List<Conversation> conversations = source.getUnreadConversationsAsList();
        source.close();

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
            return conversations.get(0).title;
        } else {
            return getResources().getQuantityString(R.plurals.new_conversations,
                    conversations.size(), conversations.size());
        }
    }

    private String getBody(List<Conversation> conversations) {
        if (conversations.size() == 1) {
            return conversations.get(0).snippet;
        } else if (conversations.size() > 1) {
            StringBuilder builder = new StringBuilder(conversations.get(0).title);
            for (int i = 1; i < conversations.size(); i++) {
                builder.append(", ").append(conversations.get(i).title);
            }
            return builder.toString();
        } else {
            return "";
        }
    }

    private Intent getIntent(List<Conversation> conversations) {
        Intent intent = new Intent(this, MessengerActivity.class);

        if (conversations.size() == 1) {
            intent.setData(Uri.parse("https://messenger.klinkerapps.com/" + conversations.get(0).id));
        }

        return intent;
    }
}