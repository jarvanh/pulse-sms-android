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
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.view.ViewBadger;

public class DualSimApplication {

    private Context context;
    private View switchSim;

    public DualSimApplication(View hiddenSwitchSimButton) {
        this.switchSim = hiddenSwitchSimButton;
        this.context = hiddenSwitchSimButton.getContext();
    }

    public void apply(final long conversationId) {
        boolean visible = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 &&
                (!Account.get(context).exists() || Account.get(context).primary)) {
            final List<SubscriptionInfo> subscriptions = DualSimUtils.get(context).getAvailableSims();

            if (subscriptions != null && subscriptions.size() > 1) {
                visible = true;
                switchSim.setVisibility(View.VISIBLE);
                final ViewBadger badger = new ViewBadger(context, switchSim);

                DataSource source = DataSource.getInstance(context);
                source.open();
                final Conversation conversation = source.getConversation(conversationId);
                source.close();

                boolean set = false;
                if (conversation != null && conversation.simSubscriptionId != null) {
                    for (int i = 0; i < subscriptions.size(); i++) {
                        if (subscriptions.get(i).getSubscriptionId() == conversation.simSubscriptionId) {
                            set = true;
                            badger.setText(String.valueOf(subscriptions.get(i).getSimSlotIndex() + 1));
                        }
                    }
                }

                if (!set) {
                    // show one for default
                    badger.setText("1");
                }

                switchSim.setOnClickListener(view -> showSimSelection(subscriptions, conversation, badger));
            }
        }

        if (!visible && switchSim.getVisibility() != View.GONE) {
            switchSim.setVisibility(View.GONE);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private void showSimSelection(final List<SubscriptionInfo> subscriptions, final Conversation conversation, final ViewBadger badger) {
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
                            badger.setText("1");
                        } else {
                            conversation.simSubscriptionId =
                                    subscriptions.get(i - 1).getSubscriptionId();
                            badger.setText("" + i);
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
