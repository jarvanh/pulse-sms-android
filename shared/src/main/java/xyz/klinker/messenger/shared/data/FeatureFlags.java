package xyz.klinker.messenger.shared.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import xyz.klinker.messenger.shared.R;

/**
 * We can use these for new features or if we want to test something quick and don't know if it
 * is going to work. These are great for quick changes. Say we have something that could cause force
 * closes or that we aren't sure users will like. We don't have to go through the play store to be
 * able to change it.
 *
 * These flags should continuously be updated. After we know the flag is no longer necessary,
 * we can remove any code here and any flag implementation to stick with the true (or false) implementation
 * of the flag.
 */
public class FeatureFlags {
    // region static initialization
    private static volatile FeatureFlags featureFlags;
    public static synchronized FeatureFlags get(Context context) {
        if (featureFlags == null) {
            featureFlags = new FeatureFlags(context);
        }

        return featureFlags;
    }
    //endregion
    // region feature flag strings
    private static final String FLAG_MESSAGING_STYLE_NOTIFICATIONS = "messaging_notifications";
    private static final String FLAG_ANDROID_WEAR_SECOND_PAGE = "wear_second_page";
    private static final String FLAG_NO_NOTIFICATION_WHEN_CONVO_OPEN = "hold_notification";
    private static final String FLAG_REORDER_CONVERSATIONS_WHEN_NEW_MESSAGE_ARRIVES = "reorder_conversations";
    private static final String FLAG_TURN_DOWN_CONTENT_OBSERVER_TIMEOUT = "content_observer_timeout";
    private static final String FLAG_REMOVE_MESSAGE_LIST_DRAWER = "remove_message_drawer";
    private static final String FLAG_ARTICLE_ENHANCER = "flag_media_enhancer";
    private static final String FLAG_FEATURE_SETTINGS = "flag_feature_settings";
    private static final String FLAG_SECURE_PRIVATE = "flag_secure_private";
    private static final String FLAG_QUICK_COMPOSE = "flag_quick_compose";
    private static final String FLAG_DELAYED_SENDING = "flag_delayed_sending";
    private static final String FLAG_CLEANUP_OLD = "flag_cleanup_old";
    private static final String FLAG_NO_GROUP_MESSAGE_COLORS_FOR_GLOBAL = "flag_global_group_colors";
    private static final String FLAG_BRING_IN_NEW_MESSAGES = "flag_bring_new_messages";
    private static final String FLAG_BRING_IN_NEW_MESSAGES_2 = "flag_bring_new_messages_3";
    private static final String FLAG_DELETED_NOTIFICATION_FCM = "flag_fcm_deleted_notification";
    private static final String FLAG_KEYBOARD_LAYOUT = "flag_keyboard_layout";
    private static final String FLAG_IMPROVE_MESSAGE_PADDING = "flag_improve_message_list";
    private static final String FLAG_NOTIFICATION_ACTIONS = "flag_notification_actions";
    private static final String FLAG_HEADS_UP = "flag_heads_up";
    private static final String FLAG_MESSAGE_REFRESH_ON_START = "flag_refresh_messages_on_start";
    private static final String FLAG_NOUGAT_NOTIFICATION_HISTORY = "flag_nougat_notifications";
    private static final String FLAG_ATTACH_CONTACT = "flag_attach_contact";
    private static final String FLAG_CONVO_LIST_CARD_ABOUT_TEXT_ANYWHERE = "flag_text_anywhere_convo_list";
    private static final String FLAG_ONLY_NOTIFY_NEW = "flag_only_notify_new";
    private static final String FLAG_IMPROVE_CONTACT_MATCH = "flag_improve_contact_match";
    // endregion

    private static final String[] ALWAYS_ON_FLAGS = new String[] {
            FLAG_IMPROVE_CONTACT_MATCH
    };

    // ADDING FEATURE FLAGS:
    // 1. Add the static identifiers and flag name right below here.
    // 2. Set up the flag in the constructor
    // 3. Add the switch case for the flag in the updateFlag method


    // step 1
    //public boolean MESSAGING_STYLE_NOTIFICATIONS;
    public boolean SECURE_PRIVATE;
    public boolean QUICK_COMPOSE;
    public boolean ONLY_NOFIY_NEW;
    public boolean IMPROVE_CONTACT_MATCH;

    private Context context;
    private FeatureFlags(final Context context) {
        this.context = context;
        SharedPreferences sharedPrefs = getSharedPrefs();

        // step 2
        //MESSAGING_STYLE_NOTIFICATIONS = getValue(sharedPrefs, FLAG_MESSAGING_STYLE_NOTIFICATIONS);
        SECURE_PRIVATE = getValue(sharedPrefs, FLAG_SECURE_PRIVATE);
        QUICK_COMPOSE = getValue(sharedPrefs, FLAG_QUICK_COMPOSE);
        ONLY_NOFIY_NEW = getValue(sharedPrefs, FLAG_ONLY_NOTIFY_NEW);
        IMPROVE_CONTACT_MATCH = getValue(sharedPrefs, FLAG_IMPROVE_CONTACT_MATCH);
    }

    public void updateFlag(String identifier, boolean flag) {
        getSharedPrefs().edit()
                .putBoolean(identifier, flag)
                .apply();

        switch (identifier) {
            // step 3
            /*case FLAG_MESSAGING_STYLE_NOTIFICATIONS:
                MESSAGING_STYLE_NOTIFICATIONS = flag;
                break;*/
            case FLAG_SECURE_PRIVATE:
                SECURE_PRIVATE = flag;
                break;
            case FLAG_QUICK_COMPOSE:
                QUICK_COMPOSE = flag;
                break;
            case FLAG_ONLY_NOTIFY_NEW:
                ONLY_NOFIY_NEW = flag;
                break;
            case FLAG_IMPROVE_CONTACT_MATCH:
                IMPROVE_CONTACT_MATCH = flag;
                break;
        }
    }

    private boolean getValue(SharedPreferences sharedPrefs, String key) {
        return context.getResources().getBoolean(R.bool.feature_flag_default) ||
                sharedPrefs.getBoolean(key, alwaysOn(key));
    }

    private SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private boolean alwaysOn(String key) {
        for (String s : ALWAYS_ON_FLAGS) {
            if (key.equals(s)) {
                return true;
            }
        }

        return false;
    }
}
