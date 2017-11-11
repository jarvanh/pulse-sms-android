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
import xyz.klinker.messenger.shared.data.Settings

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
}