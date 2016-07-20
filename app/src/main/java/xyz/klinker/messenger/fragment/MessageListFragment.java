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
import android.support.design.widget.Snackbar;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.MessageListAdapter;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.AnimationUtil;
import xyz.klinker.messenger.util.ColorUtil;
import xyz.klinker.messenger.util.PermissionsUtil;
import xyz.klinker.messenger.util.PhoneNumberUtil;
import xyz.klinker.messenger.util.SendUtil;
import xyz.klinker.messenger.view.AttachImageView;
import xyz.klinker.messenger.view.ElasticDragDismissFrameLayout;
import xyz.klinker.messenger.view.ElasticDragDismissFrameLayout.ElasticDragDismissCallback;

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

    private DataSource source;
    private View appBarLayout;
    private Toolbar toolbar;
    private View sendBar;
    private EditText messageEntry;
    private ImageButton attach;
    private FloatingActionButton send;
    private RecyclerView messageList;
    private LinearLayoutManager manager;
    private MessageListAdapter adapter;
    private View attachLayout;
    private FrameLayout attachHolder;
    private View attachButtonHolder;
    private ImageButton attachImage;
    private ImageButton captureImage;
    private ImageButton attachGif;
    private ImageButton recordVideo;
    private ImageButton recordAudio;

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
        source = DataSource.getInstance(getContext());
        source.open();

        View view = inflater.inflate(R.layout.fragment_message_list, parent, false);

        appBarLayout = view.findViewById(R.id.app_bar_layout);
        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        sendBar = view.findViewById(R.id.send_bar);
        messageEntry = (EditText) view.findViewById(R.id.message_entry);
        attach = (ImageButton) view.findViewById(R.id.attach);
        send = (FloatingActionButton) view.findViewById(R.id.send);
        messageList = (RecyclerView) view.findViewById(R.id.message_list);
        attachLayout = view.findViewById(R.id.attach_layout);
        attachHolder = (FrameLayout) view.findViewById(R.id.attach_holder);
        attachButtonHolder = view.findViewById(R.id.attach_button_holder);
        attachImage = (ImageButton) view.findViewById(R.id.attach_image);
        captureImage = (ImageButton) view.findViewById(R.id.capture_image);
        attachGif = (ImageButton) view.findViewById(R.id.attach_gif);
        recordVideo = (ImageButton) view.findViewById(R.id.record_video);
        recordAudio = (ImageButton) view.findViewById(R.id.record_audio);

        ElasticDragDismissFrameLayout frame = (ElasticDragDismissFrameLayout) view;
        frame.addListener(new ElasticDragDismissCallback() {
            @Override
            public void onDragDismissed() {
                getActivity().onBackPressed();
            }
        });

        initSendbar();
        initAttachHolder();
        initToolbar();
        initRecycler();

        AnimationUtil.animateConversationPeripheralIn(appBarLayout);
        AnimationUtil.animateConversationPeripheralIn(sendBar);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        adapter.getMessages().close();
        source.close();
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

    private void setNameAndDrawerColor(Activity activity) {
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
                messageEntry.setHint(R.string.type_message);
                nameView.setText("");
            }

            phoneNumberView.setText(phoneNumber);

            ColorUtil.adjustStatusBarColor(colorDarker, activity);
            ColorUtil.adjustDrawerColor(colorDarker, isGroup, activity);
        }
    }

    private void initSendbar() {
        String firstName = getArguments().getString(ARG_TITLE).split(" ")[0];
        if (!getArguments().getBoolean(ARG_IS_GROUP)) {
            String hint = getResources().getString(R.string.type_message_to, firstName);
            messageEntry.setHint(hint);
        } else {
            messageEntry.setHint(R.string.type_message);
        }

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

    private void initAttachHolder() {
        attachButtonHolder.setBackgroundColor(getArguments().getInt(ARG_COLOR));
        attach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (attachLayout.getVisibility() == View.VISIBLE) {
                    attachHolder.removeAllViews();
                    attachLayout.setVisibility(View.GONE);
                } else {
                    attachImage();
                    attachLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        attachImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attachImage();
            }
        });

        captureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureImage();
            }
        });

        attachGif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attachGif();
            }
        });

        recordVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordVideo();
            }
        });

        recordAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordAudio();
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
                long conversationId = getArguments().getLong(ARG_CONVERSATION_ID);
                final Cursor cursor = source.getMessages(conversationId);
                Log.v("message_load", "load took " + (
                        System.currentTimeMillis() - startTime) + " ms");

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setMessages(cursor);
                    }
                });

                source.readConversation(getContext(), conversationId);
            }
        }).start();
    }

    private void setMessages(Cursor messages) {
        if (adapter != null) {
            adapter.addMessage(messages);
        } else {
            adapter = new MessageListAdapter(messages, getArguments().getInt(ARG_COLOR),
                    getArguments().getInt(ARG_COLOR_ACCENT),
                    getArguments().getBoolean(ARG_IS_GROUP), manager);
            messageList.setAdapter(adapter);

            messageList.animate().alpha(1f).setDuration(100).setStartDelay(250).setListener(null);
        }
    }

    private void sendMessage() {
        if (PermissionsUtil.checkRequestMainPermissions(getActivity())) {
            PermissionsUtil.startMainPermissionRequest(getActivity());
        } else if (!PermissionsUtil.isDefaultSmsApp(getActivity())) {
            PermissionsUtil.setDefaultSmsApp(getActivity());
        } else {
            String message = messageEntry.getText().toString().trim();

            if (message.length() > 0) {
                final Message m = new Message();
                m.conversationId = getArguments().getLong(ARG_CONVERSATION_ID);
                m.type = Message.TYPE_SENDING;
                m.data = message;
                m.timestamp = System.currentTimeMillis();
                m.mimeType = MimeType.TEXT_PLAIN;
                m.read = true;
                m.seen = true;
                m.from = null;
                m.color = null;

                source.insertMessage(m, m.conversationId);
                loadMessages();
                messageEntry.setText(null);

                ConversationListFragment fragment = (ConversationListFragment) getActivity()
                        .getSupportFragmentManager().findFragmentById(R.id.conversation_list_container);
                if (fragment != null) {
                    fragment.notifyOfSentMessage(m);
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SendUtil.send(getContext(), m.data,
                                getArguments().getString(ARG_PHONE_NUMBERS));
                    }
                }).start();
            }
        }
    }

    private void attachImage() {
        attachHolder.removeAllViews();
        attachHolder.addView(new AttachImageView(getActivity()));
    }

    private void captureImage() {
        attachHolder.removeAllViews();

        getFragmentManager().beginTransaction().add(R.id.attach_holder,
                Camera2BasicFragment.newInstance()).commit();
    }

    private void attachGif() {
        attachHolder.removeAllViews();
        Toast.makeText(getContext(), "Not yet implemented", Toast.LENGTH_SHORT).show();
    }

    private void recordVideo() {
        attachHolder.removeAllViews();
        Toast.makeText(getContext(), "Not yet implemented", Toast.LENGTH_SHORT).show();
    }

    private void recordAudio() {
        attachHolder.removeAllViews();
        Toast.makeText(getContext(), "Not yet implemented", Toast.LENGTH_SHORT).show();
    }

}
