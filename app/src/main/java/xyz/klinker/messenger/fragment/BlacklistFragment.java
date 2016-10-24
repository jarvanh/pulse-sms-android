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

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.Set;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.BlacklistAdapter;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Blacklist;
import xyz.klinker.messenger.util.ColorUtils;
import xyz.klinker.messenger.util.PhoneNumberUtils;
import xyz.klinker.messenger.util.listener.BlacklistClickedListener;

/**
 * Fragment for displaying/managing blacklisted contacts.
 */
public class BlacklistFragment extends Fragment implements BlacklistClickedListener {

    private static final String ARG_PHONE_NUMBER = "phone_number";

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
        View view = inflater.inflate(R.layout.fragment_blacklist, parent, false);
        list = (RecyclerView) view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addBlacklist();
            }
        });
        emptyView = view.findViewById(R.id.empty_view);

        Settings settings = Settings.get(getActivity());
        if (settings.useGlobalThemeColor) {
            emptyView.setBackgroundColor(settings.globalColorSet.colorLight);
            fab.setBackgroundTintList(ColorStateList.valueOf(settings.globalColorSet.colorAccent));
            ColorUtils.changeRecyclerOverscrollColors(list, settings.globalColorSet.color);
        }

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
        new Thread(new Runnable() {
            @Override
            public void run() {
                final DataSource source = DataSource.getInstance(getActivity());
                source.open();
                final Cursor blacklists = source.getBlacklists();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setBlacklists(blacklists);
                        source.close();
                    }
                });
            }
        }).start();
    }

    private void setBlacklists(Cursor blacklists) {
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
        View layout = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_text,
                null, false);
        final EditText editText = (EditText) layout.findViewById(R.id.edit_text);
        editText.setHint(R.string.blacklist_hint);
        editText.setInputType(InputType.TYPE_CLASS_PHONE);

        new AlertDialog.Builder(getActivity())
                .setView(layout)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        addBlacklist(editText.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void addBlacklist(String phoneNumber) {
        final String cleared = PhoneNumberUtils.clearFormatting(phoneNumber);
        String formatted = PhoneNumberUtils.format(cleared);

        if (cleared.length() == 0) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.blacklist_need_number)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            addBlacklist();
                        }
                    })
                    .show();
        } else {
            String message = getString(R.string.add_blacklist, formatted);

            new AlertDialog.Builder(getActivity())
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Blacklist blacklist = new Blacklist();
                            blacklist.phoneNumber = cleared;

                            DataSource source = DataSource.getInstance(getActivity());
                            source.open();
                            source.insertBlacklist(blacklist);
                            source.close();

                            loadBlacklists();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    private void removeBlacklist(final long id, String number) {
        String message = getString(R.string.remove_blacklist, PhoneNumberUtils.format(number));

        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        DataSource source = DataSource.getInstance(getActivity());
                        source.open();
                        source.deleteBlacklist(id);
                        source.close();

                        loadBlacklists();
                    }
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
