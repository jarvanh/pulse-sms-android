package xyz.klinker.messenger.fragment.settings;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ListView;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.shared.util.DensityUtil;

/**
 * To be used with the MaterialPreferenceCategory
 */
public abstract class MaterialPreferenceFragment extends PreferenceFragment {

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView list = (ListView) view.findViewById(android.R.id.list);
        list.setBackgroundColor(getResources().getColor(R.color.drawerBackground));
        list.setDivider(new ColorDrawable(getResources().getColor(R.color.background)));
        list.setDividerHeight(DensityUtil.INSTANCE.toDp(getActivity(), 1));
    }
}
