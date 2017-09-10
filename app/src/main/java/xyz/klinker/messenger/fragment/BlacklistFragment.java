/*
 * Copyright (C) 2017 Luke Klinker
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

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.BlacklistAdapter;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Blacklist;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.listener.BlacklistClickedListener;

/**
 * Fragment for displaying/managing blacklisted contacts.
 */
public class BlacklistFragment extends Fragment implements BlacklistClickedListener {

    private static final String ARG_PHONE_NUMBER = "phone_number";

    private FragmentActivity activity;

    private RecyclerView list;
    private BlacklistAdapter adapter;
    private FloatingActionButton fab;
    private View emptyView;

    public static BlacklistFragment newInstance() {
        return BlacklistFragment.newInstance(null);
    }

    public static BlacklistFragment newInstance(String phoneNumber) {
        BlacklistFragment fragment = new BlacklistFragment();
        Bundle args = new Bundle();

        if (phoneNumber != null) {
            args.putString(ARG_PHONE_NUMBER, phoneNumber);
        }

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        this.activity = getActivity();

        View view = inflater.inflate(R.layout.fragment_blacklist, parent, false);
        list = (RecyclerView) view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(activity));
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(view1 -> addBlacklist());
        emptyView = view.findViewById(R.id.empty_view);

        Settings settings = Settings.get(activity);
        emptyView.setBackgroundColor(settings.mainColorSet.colorLight);
        fab.setBackgroundTintList(ColorStateList.valueOf(settings.mainColorSet.colorAccent));
        ColorUtils.changeRecyclerOverscrollColors(list, settings.mainColorSet.color);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadBlacklists();

        if (getArguments().containsKey(ARG_PHONE_NUMBER)) {
            addBlacklist(getArguments().getString(ARG_PHONE_NUMBER));
        }
    }

    private void loadBlacklists() {
        final Handler handler = new Handler();
        new Thread(() -> {
            if (activity == null) {
                return;
            }

            final List<Blacklist> blacklists = DataSource.INSTANCE.getBlacklistsAsList(activity);
            handler.post(() -> setBlacklists(blacklists));
        }).start();
    }

    private void setBlacklists(List<Blacklist> blacklists) {
        adapter = new BlacklistAdapter(blacklists, this);
        list.setAdapter(adapter);

        if (adapter.getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    private void addBlacklist() {
        //noinspection AndroidLintInflateParams
        View layout = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_text,
                null, false);
        final EditText editText = (EditText) layout.findViewById(R.id.edit_text);
        editText.setHint(R.string.blacklist_hint);
        editText.setInputType(InputType.TYPE_CLASS_PHONE);

        new AlertDialog.Builder(activity)
                .setView(layout)
                .setPositiveButton(R.string.add, (dialogInterface, i) -> addBlacklist(editText.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void addBlacklist(String phoneNumber) {
        final String cleared = PhoneNumberUtils.clearFormatting(phoneNumber);
        String formatted = PhoneNumberUtils.format(cleared);

        if (cleared.length() == 0) {
            new AlertDialog.Builder(activity)
                    .setMessage(R.string.blacklist_need_number)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> addBlacklist())
                    .show();
        } else {
            String message = getString(R.string.add_blacklist, formatted);

            new AlertDialog.Builder(activity)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        Blacklist blacklist = new Blacklist();
                        blacklist.phoneNumber = cleared;

                        if (activity != null) {
                            DataSource.INSTANCE.insertBlacklist(activity, blacklist);
                        }

                        loadBlacklists();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    private void removeBlacklist(final long id, String number) {
        String message = getString(R.string.remove_blacklist, PhoneNumberUtils.format(number));

        new AlertDialog.Builder(activity)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    DataSource.INSTANCE.deleteBlacklist(activity, id);

                    loadBlacklists();
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    @Override
    public void onClick(int position) {
        Blacklist blacklist = adapter.getItem(position);
        removeBlacklist(blacklist.id, blacklist.phoneNumber);
    }

}
