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
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EdgeEffect;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.MessageListAdapter;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.AnimationUtil;
import xyz.klinker.messenger.util.ColorUtil;
import xyz.klinker.messenger.util.PhoneNumberUtil;

/**
 * Fragment for displaying messages for a certain conversation.
 */
public class MessageListFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_PHONE_NUMBERS = "phone_numbers";
    private static final String ARG_COLOR = "color";
    private static final String ARG_COLOR_DARKER = "color_darker";
    private static final String ARG_COLOR_ACCENT = "color_accent";
    private static final String ARG_IS_GROUP = "is_group";
    private static final String ARG_CONVERSATION_ID = "conversation_id";

    private View appBarLayout;
    private Toolbar toolbar;
    private View sendBar;
    private EditText messageEntry;
    private ImageButton attach;
    private FloatingActionButton send;
    private RecyclerView messageList;
    private LinearLayoutManager manager;
    private MessageListAdapter adapter;

    public static MessageListFragment newInstance(Conversation conversation) {
        MessageListFragment fragment = new MessageListFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TITLE, conversation.title);
        args.putString(ARG_PHONE_NUMBERS, conversation.phoneNumbers);
        args.putInt(ARG_COLOR, conversation.colors.color);
        args.putInt(ARG_COLOR_DARKER, conversation.colors.colorDark);
        args.putInt(ARG_COLOR_ACCENT, conversation.colors.colorAccent);
        args.putBoolean(ARG_IS_GROUP, conversation.isGroup());
        args.putLong(ARG_CONVERSATION_ID, conversation.id);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
        View view = inflater.inflate(R.layout.fragment_message_list, parent, false);

        appBarLayout = view.findViewById(R.id.app_bar_layout);
        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        sendBar = view.findViewById(R.id.send_bar);
        messageEntry = (EditText) view.findViewById(R.id.message_entry);
        attach = (ImageButton) view.findViewById(R.id.attach);
        send = (FloatingActionButton) view.findViewById(R.id.send);
        messageList = (RecyclerView) view.findViewById(R.id.message_list);

        initToolbar();
        initSendbar();
        initRecycler();

        AnimationUtil.animateConversationPeripheralIn(appBarLayout);
        AnimationUtil.animateConversationPeripheralIn(sendBar);

        return view;
    }

    private void initToolbar() {
        String name = getArguments().getString(ARG_TITLE);
        int color = getArguments().getInt(ARG_COLOR);
        int colorAccent = getArguments().getInt(ARG_COLOR_ACCENT);

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

        setNameAndDrawerColor(getActivity());
        ColorUtil.setCursorDrawableColor(messageEntry, colorAccent);
    }

    public void setNameAndDrawerColor(Activity activity) {
        String name = getArguments().getString(ARG_TITLE);
        String phoneNumber = PhoneNumberUtil.format(getArguments().getString(ARG_PHONE_NUMBERS));
        int colorDarker = getArguments().getInt(ARG_COLOR_DARKER);
        boolean isGroup = getArguments().getBoolean(ARG_IS_GROUP);

        TextView nameView = (TextView) activity.findViewById(R.id.drawer_header_reveal_name);
        TextView phoneNumberView = (TextView) activity
                .findViewById(R.id.drawer_header_reveal_phone_number);

        // could be null when rotating the device
        if (nameView != null) {
            if (!name.equals(phoneNumber)) {
                nameView.setText(name);
            } else {
                nameView.setText("");
            }

            phoneNumberView.setText(phoneNumber);

            ColorUtil.adjustStatusBarColor(colorDarker, activity);
            ColorUtil.adjustDrawerColor(colorDarker, isGroup, activity);
        }
    }

    private void initSendbar() {
        String firstName = getArguments().getString(ARG_TITLE).split(" ")[0];
        String hint = getResources().getString(R.string.type_message_to, firstName);
        messageEntry.setHint(hint);

        messageEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                boolean handled = false;

                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                        keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) ||
                        actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    handled = true;
                }

                return handled;
            }
        });

        send.setBackgroundTintList(ColorStateList.valueOf(getArguments().getInt(ARG_COLOR_ACCENT)));
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
    }

    private void initRecycler() {
        messageList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private boolean invoked = false;

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // only invoke this once
                if (invoked) {
                    return;
                } else {
                    invoked = true;
                }

                try {
                    int color = getArguments().getInt(ARG_COLOR);
                    final Class<?> clazz = RecyclerView.class;

                    for (final String name : new String[] {"ensureTopGlow", "ensureBottomGlow"}) {
                        Method method = clazz.getDeclaredMethod(name);
                        method.setAccessible(true);
                        method.invoke(messageList);
                    }

                    for (final String name : new String[] {"mTopGlow", "mBottomGlow"}) {
                        final Field field = clazz.getDeclaredField(name);
                        field.setAccessible(true);
                        final Object edge = field.get(messageList);
                        final Field fEdgeEffect = edge.getClass().getDeclaredField("mEdgeEffect");
                        fEdgeEffect.setAccessible(true);
                        ((EdgeEffect) fEdgeEffect.get(edge)).setColor(color);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        manager = new LinearLayoutManager(getActivity());
        manager.setStackFromEnd(true);
        messageList.setLayoutManager(manager);

        loadMessages();
    }

    private void loadMessages() {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                DataSource source = DataSource.getInstance(getContext());
                source.open();
                final Cursor cursor = source.getMessages(getArguments().getLong(ARG_CONVERSATION_ID));
                Log.v("message_load", "load took " + (
                        System.currentTimeMillis() - startTime) + " ms");

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setMessages(cursor);
                    }
                });
            }
        }).start();
    }

    private void setMessages(Cursor messages) {
        if (adapter != null) {
            adapter.addMessage(messages);
        } else {
            adapter = new MessageListAdapter(messages,
                    getArguments().getInt(ARG_COLOR), manager);
            messageList.setAdapter(adapter);

            messageList.animate().alpha(1f).setDuration(100).setStartDelay(250).setListener(null);
        }
    }

    private void sendMessage() {
        String message = messageEntry.getText().toString().trim();

        if (message.length() > 0) {
            MatrixCursor cursor = (MatrixCursor) adapter.getMessages();
            cursor.addRow(new Object[] {
                    1,
                    1,
                    Message.TYPE_SENT,
                    message,
                    System.currentTimeMillis(),
                    "text/plain",
                    1,
                    1,
                    null
            });

            adapter.addMessage(cursor);
            messageEntry.setText(null);
        }
    }

}
