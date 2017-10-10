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
    private static final String FLAG_SNACKBAR_RECEIVED_MESSAGES = "flag_received_message_snackbar";
    private static final String FLAG_EMOJI_STYLE = "flag_emoji_style";
    private static final String FLAG_TV_MESSAGE_ENTRY = "flag_tv_message_entry";
    private static final String FLAG_DATABASE_SYNC_SERVICE_ALL = "flag_database_sync_service_all";
    private static final String FLAG_REMOVE_IMAGE_BORDERS = "flag_remove_image_borders_beta";
    private static final String FLAG_WHITE_LINK_TEXT = "flag_white_link_text";
    private static final String FLAG_AUTO_RETRY_FAILED_MESSAGES = "flag_auto_retry_failed_messages";
    private static final String FLAG_CHECK_NEW_MESSAGES_WITH_SIGNATURE = "flag_new_messages_with_signature";
    private static final String FLAG_HEADS_UP_ON_GROUP_PRIORITY = "flag_heads_up_group_priority";
    private static final String FLAG_RERECEIVE_GROUP_MESSAGE_FROM_SELF = "flag_rereceive_group_message_from_self";
    private static final String FLAG_BLACK_NAV_BAR = "flag_black_nav_bar";
    private static final String FLAG_REENABLE_SENDING_STATUS_ON_NON_PRIMARY = "flag_reenable_sending_status";
    private static final String FLAG_V_2_6_0 = "flag_v_2_6_0";
    private static final String FLAG_NEVER_SEND_FROM_WATCH = "flag_never_send_from_watch";
    // endregion

    private static final String[] ALWAYS_ON_FLAGS = new String[] {

    };

    // ADDING FEATURE FLAGS:
    // 1. Add the static identifiers and flag name right below here.
    // 2. Set up the flag in the constructor
    // 3. Add the switch case for the flag in the updateFlag method


    // disabled for future features
    public boolean SECURE_PRIVATE;
    public boolean QUICK_COMPOSE;
    public boolean CHECK_NEW_MESSAGES_WITH_SIGNATURE;

    // unlock for next update
    public boolean REMOVE_IMAGE_BORDERS;
    public boolean WHITE_LINK_TEXT;
    public boolean V_2_6_0;

    // need tested
    public boolean REENABLE_SENDING_STATUS_ON_NON_PRIMARY;
    public boolean NEVER_SEND_FROM_WATCH;

    private Context context;
    private FeatureFlags(final Context context) {
        this.context = context;
        SharedPreferences sharedPrefs = getSharedPrefs();

        SECURE_PRIVATE = getValue(sharedPrefs, FLAG_SECURE_PRIVATE);
        QUICK_COMPOSE = getValue(sharedPrefs, FLAG_QUICK_COMPOSE);
        CHECK_NEW_MESSAGES_WITH_SIGNATURE = getValue(sharedPrefs, FLAG_CHECK_NEW_MESSAGES_WITH_SIGNATURE);

        REMOVE_IMAGE_BORDERS = getValue(sharedPrefs, FLAG_REMOVE_IMAGE_BORDERS);
        WHITE_LINK_TEXT = getValue(sharedPrefs, FLAG_WHITE_LINK_TEXT);
        V_2_6_0 = getValue(sharedPrefs, FLAG_V_2_6_0);

        REENABLE_SENDING_STATUS_ON_NON_PRIMARY = getValue(sharedPrefs, FLAG_REENABLE_SENDING_STATUS_ON_NON_PRIMARY);
        NEVER_SEND_FROM_WATCH = getValue(sharedPrefs, FLAG_NEVER_SEND_FROM_WATCH);
    }

    public void updateFlag(String identifier, boolean flag) {
        getSharedPrefs().edit()
                .putBoolean(identifier, flag)
                .apply();

        switch (identifier) {
            case FLAG_SECURE_PRIVATE:
                SECURE_PRIVATE = flag;
                break;
            case FLAG_QUICK_COMPOSE:
                QUICK_COMPOSE = flag;
                break;
            case FLAG_CHECK_NEW_MESSAGES_WITH_SIGNATURE:
                CHECK_NEW_MESSAGES_WITH_SIGNATURE = flag;
                break;


            case FLAG_REMOVE_IMAGE_BORDERS:
                REMOVE_IMAGE_BORDERS = flag;
                break;
            case FLAG_WHITE_LINK_TEXT:
                WHITE_LINK_TEXT = flag;
                break;
            case FLAG_V_2_6_0:
                V_2_6_0 = flag;
                break;


            case FLAG_REENABLE_SENDING_STATUS_ON_NON_PRIMARY:
                REENABLE_SENDING_STATUS_ON_NON_PRIMARY = flag;
                break;
            case FLAG_NEVER_SEND_FROM_WATCH:
                NEVER_SEND_FROM_WATCH = flag;
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
