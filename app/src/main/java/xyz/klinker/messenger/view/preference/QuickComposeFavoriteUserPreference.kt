package xyz.klinker.messenger.view.preference

import android.app.AlertDialog
import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.MultiAutoCompleteTextView
import com.android.ex.chips.BaseRecipientAdapter
import com.android.ex.chips.RecipientEditTextView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.PhoneNumberUtils

@Suppress("DEPRECATION")
class QuickComposeFavoriteUserPreference : Preference, Preference.OnPreferenceClickListener {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val layout = LayoutInflater.from(context).inflate(R.layout.preference_quick_text_favorites, null, false)

        val contactEntryOne = layout.findViewById<RecipientEditTextView>(R.id.contact_one)
        val contactEntryTwo = layout.findViewById<RecipientEditTextView>(R.id.contact_two)
        val contactEntryThree = layout.findViewById<RecipientEditTextView>(R.id.contact_three)

        prepareContactEntry(contactEntryOne)
        prepareContactEntry(contactEntryTwo)
        prepareContactEntry(contactEntryThree)

        AlertDialog.Builder(context, R.style.SubscriptionPicker)
                .setTitle(R.string.quick_compose_favorites_title)
                .setView(layout)
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .setPositiveButton(R.string.save) { _, _ ->
                    val numbers = saveFavoritesList(contactEntryOne, contactEntryTwo, contactEntryThree)
                    ApiUtils.updateFavoriteUserNumbers(Account.accountId, numbers)
                }.show()

        return false
    }

    private fun prepareContactEntry(contactEntry: RecipientEditTextView) {
        val adapter = BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, context)
        adapter.isShowMobileOnly = Settings.mobileOnly

        contactEntry.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        contactEntry.highlightColor = Settings.mainColorSet.colorAccent
        contactEntry.setAdapter(adapter)
        contactEntry.maxChips = 1
    }

    private fun saveFavoritesList(vararg contactEntries: RecipientEditTextView): String? {
        var numbers: String? = contactEntries
                .map { if (it.recipients.isNotEmpty()) it.recipients[0].entry.destination else "" }
                .map { PhoneNumberUtils.clearFormattingAndStripStandardReplacements(it) }
                .filter { it.isNotEmpty() }
                .joinToString(",")

        if (numbers?.isEmpty() == true) {
            numbers = null
        }

        Settings.getSharedPrefs(context).edit()
                .putString(context.getString(R.string.pref_quick_compose_favorites), numbers)
                .apply()

        return numbers
    }
}