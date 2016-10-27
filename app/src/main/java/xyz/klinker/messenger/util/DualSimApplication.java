package xyz.klinker.messenger.util;


import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.preference.Preference;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.View;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.Conversation;

public class DualSimApplication {

    private Context context;
    private View switchSim;

    public DualSimApplication(View hiddenSwitchSimButton) {
        this.switchSim = hiddenSwitchSimButton;
        this.context = hiddenSwitchSimButton.getContext();
    }

    public void apply(final long conversationId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager manager = SubscriptionManager.from(context);
            final List<SubscriptionInfo> subscriptions = manager.getActiveSubscriptionInfoList();

            if (subscriptions != null && subscriptions.size() > 1) {
                switchSim.setVisibility(View.VISIBLE);

                switchSim.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DataSource source = DataSource.getInstance(context);
                        source.open();
                        Conversation conversation = source.getConversation(conversationId);
                        source.close();

                        showSimSelection(subscriptions, conversation);
                    }
                });
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private void showSimSelection(final List<SubscriptionInfo> subscriptions, final Conversation conversation) {
        final CharSequence[] active = new CharSequence[1 + subscriptions.size()];
        int selected = 0;
        active[0] = context.getString(R.string.default_text);

        for (int i = 0; i < subscriptions.size(); i++) {
            SubscriptionInfo info = subscriptions.get(i);

            active[i + 1] = formatSimString(info);
            if (conversation.simSubscriptionId != null &&
                    info.getSubscriptionId() == conversation.simSubscriptionId) {
                selected = i + 1;
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.select_sim))
                .setSingleChoiceItems(active, selected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0) {
                            conversation.simSubscriptionId = -1;
                        } else {
                            conversation.simSubscriptionId =
                                    subscriptions.get(i - 1).getSubscriptionId();
                        }

                        DataSource source = DataSource.getInstance(context);
                        source.open();
                        source.updateConversationSettings(conversation);
                        source.close();

                        dialogInterface.dismiss();
                    }
                }).show();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private String formatSimString(SubscriptionInfo info) {
        return info.getNumber() + " (SIM " + (info.getSimSlotIndex() + 1) + ")";
    }

}
