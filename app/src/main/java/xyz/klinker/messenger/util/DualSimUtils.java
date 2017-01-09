package xyz.klinker.messenger.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
public class DualSimUtils {

    private static volatile DualSimUtils dualSimUtils;

    /**
     * Gets a new instance (singleton) of Dual SIM utils.
     *
     * @param context the current application context.
     * @return the utils instance.
     */
    public static synchronized DualSimUtils get(Context context) {
        if (dualSimUtils == null) {
            dualSimUtils = new DualSimUtils(context);
        }

        return dualSimUtils;
    }

    private DualSimUtils(final Context context) {
        init(context);
    }

    private Context context;
    private List<SubscriptionInfo> availableSims;
    private SubscriptionManager manager;

    private void init(Context context) {
        this.context = context;

        if (canHandleDualSim()) {
            manager = SubscriptionManager.from(context);
            try {
                for (int i = 0; i < 100; i++) {
                    SubscriptionInfo info = manager.getActiveSubscriptionInfoForSimSlotIndex(i);
                    if (info != null) {
                        availableSims.add(info);
                    }
                }

                if (availableSims.size() <= 1) {
                    // not a dual sim phone, ignore this.
                    this.availableSims = new ArrayList<>();
                }
            } catch (Throwable t) {
                this.availableSims = new ArrayList<>();
            }
        } else {
            this.availableSims = new ArrayList<>();
        }
    }

    public List<SubscriptionInfo> getAvailableSims() {
        return availableSims;
    }

    public SubscriptionInfo getSubscriptionInfo(Integer simSubscription) {
        if (simSubscription == null || simSubscription == 0 || simSubscription == -1) {
            return null;
        }

        for (SubscriptionInfo sim : availableSims) {
            if (sim.getSubscriptionId() == simSubscription) {
                return sim;
            }
        }

        return null;
    }

    public String getNumberFromSimSlot(int simSlot) {
        for (SubscriptionInfo sim : availableSims) {
            if (sim.getSimSlotIndex() == simSlot) {
                return sim.getNumber();
            }
        }

        return null;
    }

    public String getPhoneNumberFromSimSubscription(int simSubscription) {
        if (simSubscription == 0 || simSubscription == -1) {
            return null;
        }

        for (SubscriptionInfo sim : availableSims) {
            if (sim.getSubscriptionId() == simSubscription) {
                return sim.getNumber();
            }
        }

        return null;
    }

    public String getDefaultPhoneNumber() {
        try {
            if (manager != null && manager.getActiveSubscriptionInfoCount() > 0) {
                return manager.getActiveSubscriptionInfoList().get(0).getNumber();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean canHandleDualSim() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;
    }
}
