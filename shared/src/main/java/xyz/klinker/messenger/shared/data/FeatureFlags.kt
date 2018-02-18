package xyz.klinker.messenger.shared.data

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

import xyz.klinker.messenger.shared.R

@Suppress("unused", "MayBeConstant", "MemberVisibilityCanBePrivate")
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
object FeatureFlags {

    private val FLAG_MESSAGING_STYLE_NOTIFICATIONS = "messaging_notifications"
    private val FLAG_ANDROID_WEAR_SECOND_PAGE = "wear_second_page"
    private val FLAG_NO_NOTIFICATION_WHEN_CONVO_OPEN = "hold_notification"
    private val FLAG_REORDER_CONVERSATIONS_WHEN_NEW_MESSAGE_ARRIVES = "reorder_conversations"
    private val FLAG_TURN_DOWN_CONTENT_OBSERVER_TIMEOUT = "content_observer_timeout"
    private val FLAG_REMOVE_MESSAGE_LIST_DRAWER = "remove_message_drawer"
    private val FLAG_ARTICLE_ENHANCER = "flag_media_enhancer"
    private val FLAG_FEATURE_SETTINGS = "flag_feature_settings"
    private val FLAG_SECURE_PRIVATE = "flag_secure_private"
    private val FLAG_QUICK_COMPOSE = "flag_quick_compose"
    private val FLAG_DELAYED_SENDING = "flag_delayed_sending"
    private val FLAG_CLEANUP_OLD = "flag_cleanup_old"
    private val FLAG_NO_GROUP_MESSAGE_COLORS_FOR_GLOBAL = "flag_global_group_colors"
    private val FLAG_BRING_IN_NEW_MESSAGES = "flag_bring_new_messages"
    private val FLAG_BRING_IN_NEW_MESSAGES_2 = "flag_bring_new_messages_3"
    private val FLAG_DELETED_NOTIFICATION_FCM = "flag_fcm_deleted_notification"
    private val FLAG_KEYBOARD_LAYOUT = "flag_keyboard_layout"
    private val FLAG_IMPROVE_MESSAGE_PADDING = "flag_improve_message_list"
    private val FLAG_NOTIFICATION_ACTIONS = "flag_notification_actions"
    private val FLAG_HEADS_UP = "flag_heads_up"
    private val FLAG_MESSAGE_REFRESH_ON_START = "flag_refresh_messages_on_start"
    private val FLAG_NOUGAT_NOTIFICATION_HISTORY = "flag_nougat_notifications"
    private val FLAG_ATTACH_CONTACT = "flag_attach_contact"
    private val FLAG_CONVO_LIST_CARD_ABOUT_TEXT_ANYWHERE = "flag_text_anywhere_convo_list"
    private val FLAG_ONLY_NOTIFY_NEW = "flag_only_notify_new"
    private val FLAG_IMPROVE_CONTACT_MATCH = "flag_improve_contact_match"
    private val FLAG_SNACKBAR_RECEIVED_MESSAGES = "flag_received_message_snackbar"
    private val FLAG_EMOJI_STYLE = "flag_emoji_style"
    private val FLAG_TV_MESSAGE_ENTRY = "flag_tv_message_entry"
    private val FLAG_DATABASE_SYNC_SERVICE_ALL = "flag_database_sync_service_all"
    private val FLAG_REMOVE_IMAGE_BORDERS = "flag_remove_image_borders_beta"
    private val FLAG_WHITE_LINK_TEXT = "flag_white_link_text"
    private val FLAG_AUTO_RETRY_FAILED_MESSAGES = "flag_auto_retry_failed_messages"
    private val FLAG_CHECK_NEW_MESSAGES_WITH_SIGNATURE = "flag_new_messages_with_signature"
    private val FLAG_HEADS_UP_ON_GROUP_PRIORITY = "flag_heads_up_group_priority"
    private val FLAG_RERECEIVE_GROUP_MESSAGE_FROM_SELF = "flag_rereceive_group_message_from_self"
    private val FLAG_BLACK_NAV_BAR = "flag_black_nav_bar"
    private val FLAG_REENABLE_SENDING_STATUS_ON_NON_PRIMARY = "flag_reenable_sending_status"
    private val FLAG_V_2_6_0 = "flag_v_2_6_0"
    private val FLAG_NEVER_SEND_FROM_WATCH = "flag_never_send_from_watch"
    private val FLAG_EMAIL_RECEPTION_CONVERSION = "flag_email_reception_conversion"
    private val FLAG_RECONCILE_RECEIVED_MESSAGES = "flag_reconcile_received_messages"
    private val FLAG_TEMPLATE_SUPPORT = "flag_template_support"
    private val FLAG_FOLDER_SUPPORT = "flag_folder_support"
    private val FLAG_ADJUSTABLE_NAV_BAR = "flag_adjustable_nav_bar"
    private val FLAG_WIDGET_THEMEING = "flag_widget_themeing"
    private val FLAG_MULTI_SELECT_MEDIA = "flag_multi_select_media"
    private val FLAG_SHARE_MULTIPLE_MESSAGES = "flag_share_multiple_messages"
    private val FLAG_AUTO_REPLIES = "flag_auto_replies"
    private val FLAG_VCARD_PREVIEWS = "flag_vcard_previews"
    private val FLAG_DISMISS_NOTI_BY_UNREAD_MESSAGES = "flag_dismiss_noti_by_unread_messages"


    private val ALWAYS_ON_FLAGS = listOf(FLAG_REENABLE_SENDING_STATUS_ON_NON_PRIMARY)


    // ADDING FEATURE FLAGS:
    // 1. Add the static identifiers and flag name right below here.
    // 2. Set up the flag in the constructor
    // 3. Add the switch case for the flag in the updateFlag method

    val SKIP_INTRO_PAGER = true

    // always on
    var REENABLE_SENDING_STATUS_ON_NON_PRIMARY: Boolean = false

    // disabled for future features
    var SECURE_PRIVATE: Boolean = false
    var FOLDER_SUPPORT: Boolean = false
    var WIDGET_THEMEING: Boolean = false
    var MULTI_SELECT_MEDIA: Boolean = false
    var SHARE_MULTIPLE_MESSAGES: Boolean = false
    var AUTO_REPLIES: Boolean = false
    var VCARD_PREVIEWS: Boolean = false

    // in testing

    fun init(context: Context) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        REENABLE_SENDING_STATUS_ON_NON_PRIMARY = getValue(context, sharedPrefs, FLAG_REENABLE_SENDING_STATUS_ON_NON_PRIMARY)

        SECURE_PRIVATE = getValue(context, sharedPrefs, FLAG_SECURE_PRIVATE)
        FOLDER_SUPPORT = getValue(context, sharedPrefs, FLAG_FOLDER_SUPPORT)
        WIDGET_THEMEING = getValue(context, sharedPrefs, FLAG_WIDGET_THEMEING)
        MULTI_SELECT_MEDIA = getValue(context, sharedPrefs, FLAG_MULTI_SELECT_MEDIA)
        SHARE_MULTIPLE_MESSAGES = getValue(context, sharedPrefs, FLAG_SHARE_MULTIPLE_MESSAGES)
        AUTO_REPLIES = getValue(context, sharedPrefs, FLAG_AUTO_REPLIES)
        VCARD_PREVIEWS = getValue(context, sharedPrefs, FLAG_VCARD_PREVIEWS)
    }

    fun updateFlag(context: Context, identifier: String, flag: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(identifier, flag)
                .apply()

        when (identifier) {
            FLAG_REENABLE_SENDING_STATUS_ON_NON_PRIMARY -> REENABLE_SENDING_STATUS_ON_NON_PRIMARY = flag

            FLAG_SECURE_PRIVATE -> SECURE_PRIVATE = flag
            FLAG_FOLDER_SUPPORT -> FOLDER_SUPPORT = flag
            FLAG_WIDGET_THEMEING -> WIDGET_THEMEING = flag
            FLAG_MULTI_SELECT_MEDIA -> MULTI_SELECT_MEDIA = flag
            FLAG_SHARE_MULTIPLE_MESSAGES -> SHARE_MULTIPLE_MESSAGES = flag
            FLAG_AUTO_REPLIES -> AUTO_REPLIES = flag
            FLAG_VCARD_PREVIEWS -> VCARD_PREVIEWS = flag
        }
    }

    private fun getValue(context: Context, sharedPrefs: SharedPreferences, key: String) =
            context.resources.getBoolean(R.bool.feature_flag_default) || sharedPrefs.getBoolean(key, alwaysOn(key))

    private fun alwaysOn(key: String) = ALWAYS_ON_FLAGS.contains(key)
}
