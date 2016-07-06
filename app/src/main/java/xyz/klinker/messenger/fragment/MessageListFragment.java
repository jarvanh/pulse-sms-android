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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.Contact;
import xyz.klinker.messenger.util.ColorUtil;

/**
 * Fragment for displaying messages for a certain conversation.
 */
public class MessageListFragment extends Fragment {

    private static final String ARG_NAME = "name";
    private static final String ARG_PHONE_NUMBER = "phone_number";
    private static final String ARG_COLOR = "color";
    private static final String ARG_COLOR_DARKER = "color_darker";

    private AppBarLayout appBarLayout;
    private Toolbar toolbar;

    public static MessageListFragment newInstance(Contact contact) {
        MessageListFragment fragment = new MessageListFragment();

        Bundle args = new Bundle();
        args.putString(ARG_NAME, contact.name);
        args.putString(ARG_PHONE_NUMBER, contact.phoneNumber);
        args.putInt(ARG_COLOR, contact.color);
        args.putInt(ARG_COLOR_DARKER, contact.colorDarker);
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
        String name = getArguments().getString(ARG_NAME);
        String phoneNumber = getArguments().getString(ARG_PHONE_NUMBER);
        int color = getArguments().getInt(ARG_COLOR);
        int colorDarker = getArguments().getInt(ARG_COLOR_DARKER);

        Log.v("MessageListFragment", name + ": " + phoneNumber);

        toolbar.setTitle(name);
        toolbar.setBackgroundColor(color);

        if (!getResources().getBoolean(R.bool.pin_drawer)) {
            final DrawerLayout drawerLayout = (DrawerLayout) getActivity()
                    .findViewById(R.id.drawer_layout);
            toolbar.setNavigationIcon(R.drawable.ic_menu);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        TextView nameView = (TextView) getActivity().findViewById(R.id.name);
        TextView phoneNumberView = (TextView) getActivity().findViewById(R.id.phone_number);
        nameView.setText(name);
        phoneNumberView.setText(phoneNumber);

        ColorUtil.adjustStatusBarColor(colorDarker, getActivity());
    }

}
