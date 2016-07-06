/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.fragment;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import xyz.klinker.messenger.R;

/**
 * Fragment for displaying messages for a certain conversation.
 */
public class MessageListFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_COLOR = "color";

    private AppBarLayout appBarLayout;
    private Toolbar toolbar;

    public static MessageListFragment newInstance(String title, int color) {
        MessageListFragment fragment = new MessageListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_COLOR, color);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
        View view = inflater.inflate(R.layout.fragment_message_list, parent, false);

        appBarLayout = (AppBarLayout) view.findViewById(R.id.app_bar_layout);
        toolbar = (Toolbar) view.findViewById(R.id.toolbar);

        initToolbar();

        appBarLayout.animate().alpha(1f).translationY(0).setDuration(250)
                .setStartDelay(75).setInterpolator(new DecelerateInterpolator()).setListener(null);

        return view;
    }

    private void initToolbar() {
        toolbar.setTitle(getArguments().getString(ARG_TITLE));
        toolbar.setBackgroundColor(getArguments().getInt(ARG_COLOR));

        if (!getResources().getBoolean(R.bool.pin_drawer)) {
            toolbar.setNavigationIcon(R.drawable.ic_menu);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DrawerLayout drawerLayout = (DrawerLayout) getActivity()
                            .findViewById(R.id.drawer_layout);
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
    }

}
