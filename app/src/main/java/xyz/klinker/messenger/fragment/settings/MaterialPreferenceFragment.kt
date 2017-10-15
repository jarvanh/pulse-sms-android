package xyz.klinker.messenger.fragment.settings

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.preference.PreferenceFragment
import android.view.View
import android.widget.ListView

import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.util.DensityUtil

/**
 * To be used with the MaterialPreferenceCategory
 */
@Suppress("DEPRECATION")
abstract class MaterialPreferenceFragment : PreferenceFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val list = view.findViewById<View>(android.R.id.list) as ListView
        list.setBackgroundColor(resources.getColor(R.color.drawerBackground))
        list.divider = ColorDrawable(resources.getColor(R.color.background))
        list.dividerHeight = DensityUtil.toDp(activity, 1)
    }
}
