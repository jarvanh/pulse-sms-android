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

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.InviteFriendsAdapter;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.ImageUtils;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.SendUtils;
import xyz.klinker.messenger.shared.util.listener.ContactClickedListener;

/**
 * Fragment for inviting friends to the app.
 */
public class InviteFriendsFragment extends Fragment implements ContactClickedListener {

    private FragmentActivity activity;

    private RecyclerView list;
    private FloatingActionButton fab;
    private ProgressBar progress;
    private List<String> phoneNumbers;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        this.activity = getActivity();

        View view = inflater.inflate(R.layout.fragment_invite_friends, parent, false);

        list = (RecyclerView) view.findViewById(R.id.list);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        progress = (ProgressBar) view.findViewById(R.id.progress);

        fab.hide();
        list.setLayoutManager(new LinearLayoutManager(activity));

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fab.setOnClickListener(view1 -> sendMessage());

        loadContacts();
    }

    private void loadContacts() {
        final Handler handler = new Handler();
        if (activity == null) {
            return;
        }

        new Thread(() -> {
            Cursor cursor = activity.getContentResolver()
                    .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            new String[]{
                                    ContactsContract.CommonDataKinds.Phone._ID,
                                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                    ContactsContract.CommonDataKinds.Phone.NUMBER}
                            , null, null,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

            final List<Conversation> contacts = new ArrayList<>();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        Conversation conversation = new Conversation();
                        conversation.title = cursor.getString(1);
                        conversation.phoneNumbers = PhoneNumberUtils
                                .clearFormatting(cursor.getString(2));
                        conversation.imageUri = ContactUtils
                                .findImageUri(conversation.phoneNumbers, activity);
                        conversation.simSubscriptionId = -1;

                        Bitmap image = ImageUtils.getContactImage(conversation.imageUri, activity);
                        if (image == null) {
                            conversation.imageUri = null;
                        } else {
                            image.recycle();
                        }

                        if (contacts.size() == 0 ||
                                !conversation.title.equals(contacts.get(contacts.size() - 1).title)) {
                            contacts.add(conversation);
                        }
                    } catch (NullPointerException e) {
                        return;
                    }
                } while (cursor.moveToNext());
            }

            try {
                cursor.close();
            } catch (Exception e) { }

            handler.post(() -> {
                if (activity != null) {
                    setContacts(contacts);
                }
            });
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
            new SendUtils().send(activity, getString(R.string.invite_friends_sms), number);
        }

        activity.onBackPressed();
    }

    private void unlockFreeMonth() {
        // TODO
    }

}
