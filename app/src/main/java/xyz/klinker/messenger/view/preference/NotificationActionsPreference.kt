package xyz.klinker.messenger.view.preference

import android.app.AlertDialog
import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Spinner
import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.NotificationAction

@Suppress("DEPRECATION")
class NotificationActionsPreference : Preference, Preference.OnPreferenceClickListener {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val layout = LayoutInflater.from(context).inflate(R.layout.preference_notification_actions, null, false)

        val actionOne = layout.findViewById<Spinner>(R.id.action_one)
        val actionTwo = layout.findViewById<Spinner>(R.id.action_two)
        val actionThree = layout.findViewById<Spinner>(R.id.action_three)

        val prefActions = NotificationActionsPreference.getActionsFromPref(context)

        prepareContactEntry(actionOne, prefActions[0])
        prepareContactEntry(actionTwo, prefActions[1])
        prepareContactEntry(actionThree, prefActions[2])

        AlertDialog.Builder(context, R.style.SubscriptionPicker)
                .setTitle(R.string.notification_actions)
                .setView(layout)
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .setPositiveButton(R.string.save) { _, _ ->
                    val numbers = saveActionsList(actionOne, actionTwo, actionThree)
                    ApiUtils.updateNotificationActionsSelectable(Account.accountId, numbers)
                }.show()

        return false
    }

    private fun prepareContactEntry(spinner: Spinner, action: NotificationAction) {
        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, context.resources.getStringArray(R.array.notification_actions))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(mapActionToArrayIndex(action))
    }

    private fun saveActionsList(vararg actionEntries: Spinner): String {
        val valuesArray = context.resources.getStringArray(R.array.notification_actions_values)
        val actions = actionEntries.map { valuesArray[it.selectedItemPosition] }
        val string = buildActionsString(actions[0], actions[1], actions[2])

        Settings.getSharedPrefs(context).edit()
                .putString(context.getString(R.string.pref_notification_actions_selection), string)
                .apply()

        return string
    }

    companion object {
        fun getActionsFromPref(context: Context): List<NotificationAction> = Settings.notificationActions
        fun buildActionsString(one: String, two: String, three: String) = "$one,$two,$three"

        fun mapActionToArrayIndex(action: NotificationAction) = when (action) {
            NotificationAction.REPLY -> 0
            NotificationAction.SMART_REPLY -> 1
            NotificationAction.CALL -> 2
            NotificationAction.DELETE -> 3
            NotificationAction.READ -> 4
            NotificationAction.MUTE -> 5
            NotificationAction.ARCHIVE -> 6
            NotificationAction.EMPTY -> 7
        }
    }
}