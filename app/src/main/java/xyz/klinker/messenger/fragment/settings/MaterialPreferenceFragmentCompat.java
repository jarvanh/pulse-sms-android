package xyz.klinker.messenger.fragment.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.View;

import xyz.klinker.messenger.R;

public abstract class MaterialPreferenceFragmentCompat extends PreferenceFragmentCompat {

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setBackgroundColor(getResources().getColor(R.color.drawerBackground));
    }
}
