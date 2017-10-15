package xyz.klinker.messenger.fragment.settings

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.View

import xyz.klinker.messenger.R

@Suppress("DEPRECATION")
abstract class MaterialPreferenceFragmentCompat : PreferenceFragmentCompat() {

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.setBackgroundColor(resources.getColor(R.color.drawerBackground))
    }
}
