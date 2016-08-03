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

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ContactAdapter;
import xyz.klinker.messenger.adapter.InviteFriendsAdapter;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.util.ContactUtils;
import xyz.klinker.messenger.util.ImageUtils;
import xyz.klinker.messenger.util.PhoneNumberUtils;
import xyz.klinker.messenger.util.SendUtils;
import xyz.klinker.messenger.util.listener.ContactClickedListener;

/**
 * Fragment for inviting friends to the app.
 */
public class InviteFriendsFragment extends Fragment implements ContactClickedListener {

    private RecyclerView list;
    private FloatingActionButton fab;
    private ProgressBar progress;
    private List<String> phoneNumbers;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_invite_friends, parent, false);

        list = (RecyclerView) view.findViewById(R.id.list);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        progress = (ProgressBar) view.findViewById(R.id.progress);

        fab.hide();
        list.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        loadContacts();
    }

    private void loadContacts() {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = getActivity().getContentResolver()
                        .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                new String[] {
                                        ContactsContract.CommonDataKinds.Phone._ID,
                                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                        ContactsContract.CommonDataKinds.Phone.NUMBER}
                                , null, null,
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

                final List<Conversation> contacts = new ArrayList<>();
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        Conversation conversation = new Conversation();
                        conversation.title = cursor.getString(1);
                        conversation.phoneNumbers = PhoneNumberUtils
                                .clearFormatting(cursor.getString(2));
                        conversation.imageUri = ContactUtils
                                .findImageUri(conversation.phoneNumbers, getActivity());

                        if (ImageUtils.getContactImage(conversation.imageUri, getContext()) == null) {
                            conversation.imageUri = null;
                        }

                        contacts.add(conversation);
                    } while (cursor.moveToNext());
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setContacts(contacts);
                    }
                });
            }
        }).start();
    }

    private void setContacts(List<Conversation> contacts) {
        progress.setVisibility(View.GONE);
        phoneNumbers = new ArrayList<>();
        list.setAdapter(new InviteFriendsAdapter(contacts, this, phoneNumbers));
    }

    @Override
    public void onClicked(String title, String phoneNumber, String imageUri) {
        if (phoneNumbers.contains(phoneNumber)) {
            phoneNumbers.remove(phoneNumber);
        } else {
            phoneNumbers.add(phoneNumber);
        }

        if (phoneNumbers.size() > 0) {
            fab.show();
        } else {
            fab.hide();
        }
    }

    private void sendMessage() {
        if (phoneNumbers.size() > 10) {
            unlockFreeMonth();
        }

        for (String number : phoneNumbers) {
            SendUtils.send(getActivity(), getString(R.string.invite_friends_sms), number);
        }

        getActivity().onBackPressed();
    }

    private void unlockFreeMonth() {
        // TODO
    }

}
