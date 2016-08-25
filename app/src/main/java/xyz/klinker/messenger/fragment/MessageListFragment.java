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

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialcamera.MaterialCamera;
import com.bumptech.glide.Glide;

import java.util.List;

import xyz.klinker.giphy.Giphy;
import xyz.klinker.messenger.BuildConfig;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.MessageListAdapter;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Draft;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.receiver.MessageListUpdatedReceiver;
import xyz.klinker.messenger.util.AnimationUtils;
import xyz.klinker.messenger.util.ColorUtils;
import xyz.klinker.messenger.util.PermissionsUtils;
import xyz.klinker.messenger.util.PhoneNumberUtils;
import xyz.klinker.messenger.util.SendUtils;
import xyz.klinker.messenger.util.listener.AudioRecordedListener;
import xyz.klinker.messenger.util.listener.ImageSelectedListener;
import xyz.klinker.messenger.view.AttachImageView;
import xyz.klinker.messenger.view.ElasticDragDismissFrameLayout;
import xyz.klinker.messenger.view.ElasticDragDismissFrameLayout.ElasticDragDismissCallback;
import xyz.klinker.messenger.view.RecordAudioView;

import static android.app.Activity.RESULT_OK;

/**
 * Fragment for displaying messages for a certain conversation.
 */
public class MessageListFragment extends Fragment implements
        ImageSelectedListener, AudioRecordedListener {

    private static final String ARG_TITLE = "title";
    private static final String ARG_PHONE_NUMBERS = "phone_numbers";
    private static final String ARG_COLOR = "color";
    private static final String ARG_COLOR_DARKER = "color_darker";
    private static final String ARG_COLOR_ACCENT = "color_accent";
    private static final String ARG_IS_GROUP = "is_group";
    private static final String ARG_CONVERSATION_ID = "conversation_id";
    private static final String ARG_MUTE_CONVERSATION = "mute_conversation";
    private static final String ARG_MESSAGE_TO_OPEN_ID = "message_to_open";
    private static final String ARG_READ = "read";

    private static final int PERMISSION_STORAGE_REQUEST = 1;
    private static final int PERMISSION_AUDIO_REQUEST = 2;
    private static final int RESULT_VIDEO_REQUEST = 3;
    private static final int RESULT_GIPHY_REQUEST = 4;

    private DataSource source;
    private View appBarLayout;
    private Toolbar toolbar;
    private View sendBar;
    private EditText messageEntry;
    private ImageButton attach;
    private FloatingActionButton send;
    private TextView counter;
    private RecyclerView messageList;
    private LinearLayoutManager manager;
    private MessageListAdapter adapter;
    private ElasticDragDismissFrameLayout dragDismissFrameLayout;
    private View attachLayout;
    private FrameLayout attachHolder;
    private LinearLayout attachButtonHolder;
    private ImageButton attachImage;
    private ImageButton captureImage;
    private ImageButton attachGif;
    private ImageButton recordVideo;
    private ImageButton recordAudio;
    private View attachedImageHolder;
    private ImageView attachedImage;
    private View removeImage;
    private BroadcastReceiver updatedReceiver;
    private boolean dismissNotification = false;

    private Uri attachedUri;
    private String attachedMimeType;

    private AlertDialog detailsChoiceDialog;

    public static MessageListFragment newInstance(Conversation conversation) {
        return newInstance(conversation, -1);
    }

    public static MessageListFragment newInstance(Conversation conversation, long messageToOpenId) {
        MessageListFragment fragment = new MessageListFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TITLE, conversation.title);
        args.putString(ARG_PHONE_NUMBERS, conversation.phoneNumbers);
        args.putInt(ARG_COLOR, conversation.colors.color);
        args.putInt(ARG_COLOR_DARKER, conversation.colors.colorDark);
        args.putInt(ARG_COLOR_ACCENT, conversation.colors.colorAccent);
        args.putBoolean(ARG_IS_GROUP, conversation.isGroup());
        args.putLong(ARG_CONVERSATION_ID, conversation.id);
        args.putBoolean(ARG_MUTE_CONVERSATION, conversation.mute);
        args.putBoolean(ARG_READ, conversation.read);

        if (messageToOpenId != -1) {
            args.putLong(ARG_MESSAGE_TO_OPEN_ID, messageToOpenId);
        }

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
        counter = (TextView) view.findViewById(R.id.text_counter);
        messageList = (RecyclerView) view.findViewById(R.id.message_list);
        attachLayout = view.findViewById(R.id.attach_layout);
        attachHolder = (FrameLayout) view.findViewById(R.id.attach_holder);
        attachButtonHolder = (LinearLayout) view.findViewById(R.id.attach_button_holder);
        attachImage = (ImageButton) view.findViewById(R.id.attach_image);
        captureImage = (ImageButton) view.findViewById(R.id.capture_image);
        attachGif = (ImageButton) view.findViewById(R.id.attach_gif);
        recordVideo = (ImageButton) view.findViewById(R.id.record_video);
        recordAudio = (ImageButton) view.findViewById(R.id.record_audio);
        attachedImageHolder = view.findViewById(R.id.attached_image_holder);
        attachedImage = (ImageView) view.findViewById(R.id.attached_image);
        removeImage = view.findViewById(R.id.remove_image);

        dragDismissFrameLayout = (ElasticDragDismissFrameLayout) view;
        dragDismissFrameLayout.addListener(new ElasticDragDismissCallback() {
            @Override
            public void onDragDismissed() {
                dismissKeyboard();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getActivity().onBackPressed();
                    }
                }, 100);
            }
        });

        initSendbar();
        initAttachHolder();
        initToolbar();
        initRecycler();

        dismissNotification();

        AnimationUtils.animateConversationPeripheralIn(appBarLayout);
        AnimationUtils.animateConversationPeripheralIn(sendBar);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updatedReceiver = new MessageListUpdatedReceiver(this);
        getActivity().registerReceiver(updatedReceiver,
                MessageListUpdatedReceiver.getIntentFilter());
    }

    @Override
    public void onStart() {
        super.onStart();
        dismissNotification = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        dismissNotification = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (updatedReceiver != null) {
            getActivity().unregisterReceiver(updatedReceiver);
        }

        if (messageEntry.getText() != null && messageEntry.getText().length() > 0) {
            source.insertDraft(getConversationId(),
                    messageEntry.getText().toString(), MimeType.TEXT_PLAIN);
        }

        if (attachedUri != null) {
            source.insertDraft(getConversationId(), attachedUri.toString(), attachedMimeType);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (adapter != null) {
            adapter.getMessages().close();
        }

        if (source != null) {
            source.close();
        }
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
        ColorUtils.setCursorDrawableColor(messageEntry, colorAccent);
    }

    private void setNameAndDrawerColor(Activity activity) {
        String name = getArguments().getString(ARG_TITLE);
        String phoneNumber = PhoneNumberUtils.format(getArguments().getString(ARG_PHONE_NUMBERS));
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

            ColorUtils.adjustStatusBarColor(colorDarker, activity);
            ColorUtils.adjustDrawerColor(colorDarker, isGroup, activity);
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

                if ((keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                        keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) ||
                        actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    handled = true;
                }

                return handled;
            }
        });

        messageEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (attachLayout.getVisibility() == View.VISIBLE) {
                    attach.performClick();
                }
            }
        });

        messageEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                changeCounterText();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        int accent = getArguments().getInt(ARG_COLOR_ACCENT);
        send.setBackgroundTintList(ColorStateList.valueOf(accent));
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
        messageEntry.setHighlightColor(accent);

        removeImage.setBackgroundColor(accent);
        removeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearAttachedData();
            }
        });
    }

    private void changeCounterText() {
        if (attachedUri == null && !getArguments().getBoolean(ARG_IS_GROUP)) {
            int[] count = SmsMessage.calculateLength(messageEntry.getText().toString(), false);

            if ((count[0] > 1 && count[0] < 4) || (count[0] == 1 && count[2] < 30)) {
                //noinspection AndroidLintSetTextI18n
                counter.setText(count[0] + "/" + count[2]);
            } else {
                if (count[0] >= 4) {
                    counter.setText(/*R.string.mms_message*/ null);
                } else {
                    counter.setText(null);
                }
            }
        } else {
            counter.setText(/*R.string.mms_message*/ null);
        }
    }

    private void initAttachHolder() {
        attachButtonHolder.setBackgroundColor(getArguments().getInt(ARG_COLOR));
        attach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ValueAnimator animator;

                if (attachLayout.getVisibility() == View.VISIBLE) {
                    dragDismissFrameLayout.setEnabled(true);
                    animator = ValueAnimator.ofInt(attachLayout.getHeight(), 0);
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            attachHolder.removeAllViews();
                            attachLayout.setVisibility(View.GONE);
                        }
                    });
                } else {
                    dragDismissFrameLayout.setEnabled(false);
                    attachImage();
                    attachLayout.setVisibility(View.VISIBLE);
                    animator = ValueAnimator.ofInt(0,
                            getResources().getDimensionPixelSize(R.dimen.attach_menu_height));
                }

                final ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) attachLayout.getLayoutParams();

                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        params.height = (Integer) valueAnimator.getAnimatedValue();
                        attachLayout.requestLayout();
                    }
                });

                animator.setDuration(200);
                animator.start();
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
        ColorUtils.changeRecyclerOverscrollColors(messageList, getArguments().getInt(ARG_COLOR));

        manager = new LinearLayoutManager(getActivity());
        manager.setStackFromEnd(true);
        messageList.setLayoutManager(manager);

        loadMessages();
    }

    private void dismissNotification() {
        try {
            if (dismissNotification) {
                NotificationManagerCompat.from(getContext())
                        .cancel((int) getConversationId());
                new ApiUtils().dismissNotification(Settings.get(getActivity()).accountId,
                        getConversationId());
            }
        } catch (Exception e) {

        }
    }

    public void loadMessages() {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                long conversationId = getConversationId();

                if (source.isOpen()) {
                    long startTime = System.currentTimeMillis();
                    final Cursor cursor = source.getMessages(conversationId);
                    final List<Draft> drafts = source.getDrafts(conversationId);

                    if (drafts.size() > 0) {
                        source.deleteDrafts(conversationId);
                    }

                    final int position = findMessagePositionFromId(cursor);

                    Log.v("message_load", "load took " + (
                            System.currentTimeMillis() - startTime) + " ms");

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            setMessages(cursor);
                            setDrafts(drafts);

                            if (position != -1) {
                                messageList.scrollToPosition(position);
                            }
                        }
                    });
                }

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }

                try {
                    if (source.isOpen()) {
                        dismissNotification();
                        source.readConversation(getContext(), conversationId);
                    }
                } catch (IllegalStateException e) {

                }
            }
        }).start();
    }

    private int findMessagePositionFromId(Cursor cursor) {
        if (getArguments() != null && getArguments()
                .containsKey(ARG_MESSAGE_TO_OPEN_ID) &&
                cursor != null && cursor.moveToFirst()) {
            long id = getArguments().getLong(ARG_MESSAGE_TO_OPEN_ID);

            do {
                if (cursor.getLong(0) == id) {
                    return cursor.getPosition();
                }
            } while (cursor.moveToNext());
        }

        return -1;
    }

    private void setMessages(Cursor messages) {
        if (adapter != null) {
            adapter.addMessage(messages);
        } else {
            adapter = new MessageListAdapter(messages, getArguments().getInt(ARG_COLOR),
                    getArguments().getInt(ARG_COLOR_ACCENT),
                    getArguments().getBoolean(ARG_IS_GROUP), manager, this);
            messageList.setAdapter(adapter);

            messageList.animate().withLayer()
                    .alpha(1f).setDuration(100).setStartDelay(250).setListener(null);
        }
    }

    private void setDrafts(List<Draft> drafts) {
        for (Draft draft : drafts) {
            if (draft.mimeType.equals(MimeType.TEXT_PLAIN)) {
                messageEntry.setText(draft.data);
            } else if (MimeType.isStaticImage(draft.mimeType)) {
                attachImage(Uri.parse(draft.data));
            }
        }
    }

    private void sendMessage() {
        if (PermissionsUtils.checkRequestMainPermissions(getActivity())) {
            PermissionsUtils.startMainPermissionRequest(getActivity());
        } else if (Settings.get(getActivity()).primary &&
                !PermissionsUtils.isDefaultSmsApp(getActivity())) {
            PermissionsUtils.setDefaultSmsApp(getActivity());
        } else {
            final String message = messageEntry.getText().toString().trim();
            final Uri uri = attachedUri;
            final String mimeType = attachedMimeType;

            if (message.length() > 0 || attachedUri != null) {
                final Message m = new Message();
                m.conversationId = getConversationId();
                m.type = Message.TYPE_SENDING;
                m.data = message;
                m.timestamp = System.currentTimeMillis();
                m.mimeType = MimeType.TEXT_PLAIN;
                m.read = true;
                m.seen = true;
                m.from = null;
                m.color = null;

                if (adapter.getItemCount() == 1 &&
                        adapter.getItemViewType(0) == Message.TYPE_INFO) {
                    source.deleteMessage(adapter.getItemId(0));
                }

                if (message.length() != 0) {
                    source.insertMessage(getActivity(), m, m.conversationId);
                    loadMessages();
                }

                messageEntry.setText(null);

                ConversationListFragment fragment = (ConversationListFragment) getActivity()
                        .getSupportFragmentManager().findFragmentById(R.id.conversation_list_container);

                if (fragment != null) {
                    fragment.notifyOfSentMessage(m);
                }

                if (uri != null) {
                    m.data = uri.toString();
                    m.mimeType = mimeType;

                    if (m.id != 0) {
                        m.id = 0;
                    }

                    source.insertMessage(getActivity(), m, m.conversationId);
                    loadMessages();
                }

                clearAttachedData();

                if (fragment != null) {
                    fragment.notifyOfSentMessage(m);
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Uri imageUri = SendUtils.send(getContext(), message,
                                getArguments().getString(ARG_PHONE_NUMBERS), uri, mimeType);
                        source.deleteDrafts(getConversationId());

                        if (imageUri != null && uri != null) {
                            Cursor cursor = source.searchMessages(uri.toString());
                            if (cursor != null && cursor.moveToFirst()) {
                                source.updateMessageData(cursor.getLong(0), imageUri.toString());
                                cursor.close();
                            }
                        }
                    }
                }).start();
            }
        }
    }

    private void attachImage() {
        prepareAttachHolder(0);
        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            attachHolder.addView(new AttachImageView(getActivity(), this,
                    getArguments().getInt(ARG_COLOR)));
        } else {
            attachPermissionRequest(PERMISSION_STORAGE_REQUEST,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void captureImage() {
        prepareAttachHolder(1);

        Camera2BasicFragment fragment = Camera2BasicFragment.newInstance();
        getFragmentManager().beginTransaction().add(R.id.attach_holder, fragment).commit();
        fragment.attachImageSelectedListener(this);
    }

    private void attachGif() {
        prepareAttachHolder(2);
        new Giphy.Builder(getActivity(), BuildConfig.GIPHY_API_KEY)
                .maxFileSize(1024 * 1024)
                .start(RESULT_GIPHY_REQUEST);
    }

    private void recordVideo() {
        prepareAttachHolder(3);

        new MaterialCamera(getActivity())
                .saveDir(getActivity().getFilesDir().getPath())
                .qualityProfile(MaterialCamera.QUALITY_LOW)
                .maxAllowedFileSize(1024 * 1024)
                .allowRetry(false)
                .autoSubmit(true)
                .showPortraitWarning(false)
                .start(RESULT_VIDEO_REQUEST);
    }

    private void recordAudio() {
        prepareAttachHolder(4);
        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            attachHolder.addView(new RecordAudioView(getActivity(), this,
                    getArguments().getInt(ARG_COLOR_ACCENT)));
        } else {
            attachPermissionRequest(PERMISSION_AUDIO_REQUEST,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO);
        }
    }

    private void attachPermissionRequest(final int permissionRequestCode, final String... permissions) {
        getLayoutInflater(null).inflate(R.layout.permission_request, attachHolder, true);
        Button request = (Button) attachHolder.findViewById(R.id.permission_needed);
        request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermissions(permissions, permissionRequestCode);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_STORAGE_REQUEST) {
            attachImage();
        } else if (requestCode == PERMISSION_AUDIO_REQUEST) {
            recordAudio();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_VIDEO_REQUEST) {
            onBackPressed();

            if (resultCode == RESULT_OK) {
                Log.v("video result", "saved to " + data.getDataString());
                attachImage(data.getData());
                attachedMimeType = MimeType.VIDEO_MP4;
            } else if (data != null) {
                Exception e = (Exception) data.getSerializableExtra(MaterialCamera.ERROR_EXTRA);
                e.printStackTrace();
                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == RESULT_GIPHY_REQUEST) {
            onBackPressed();

            if (resultCode == RESULT_OK) {
                Log.v("gif result", "saved to " + data.getDataString());
                attachImage(data.getData());
                attachedMimeType = MimeType.IMAGE_GIF;
            }
        }
    }

    private void prepareAttachHolder(int positionToBold) {
        dismissKeyboard();
        attachHolder.removeAllViews();

        for (int i = 0; i < attachButtonHolder.getChildCount(); i++) {
            if (positionToBold == i) {
                attachButtonHolder.getChildAt(i).setAlpha(1.0f);
            } else {
                attachButtonHolder.getChildAt(i).setAlpha(0.5f);
            }
        }
    }

    private void clearAttachedData() {
        attachedImageHolder.setVisibility(View.GONE);
        attachedImage.setImageDrawable(null);
        attachedUri = null;
        attachedMimeType = null;
        changeCounterText();
    }

    private void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(messageEntry.getWindowToken(), 0);
    }

    @Override
    public void onImageSelected(Uri uri) {
        onBackPressed();
        attachImage(uri);
    }

    @Override
    public void onRecorded(Uri uri) {
        onBackPressed();
        attachAudio(uri);
    }

    private void attachImage(Uri uri) {
        clearAttachedData();
        attachedUri = uri;
        attachedMimeType = MimeType.IMAGE_JPG;

        attachedImageHolder.setVisibility(View.VISIBLE);
        Glide.with(getContext())
                .load(uri)
                .placeholder(R.drawable.ic_image_sending)
                .into(attachedImage);
        changeCounterText();
    }

    private void attachAudio(Uri uri) {
        clearAttachedData();
        attachedUri = uri;
        attachedMimeType = MimeType.AUDIO_MP4;

        attachedImageHolder.setVisibility(View.VISIBLE);
        attachedImage.setImageResource(R.drawable.ic_audio_sent);
        changeCounterText();
    }

    public boolean onBackPressed() {
        if (detailsChoiceDialog != null && detailsChoiceDialog.isShowing()) {
            detailsChoiceDialog.dismiss();
            detailsChoiceDialog = null;
        }

        if (attachLayout.getVisibility() == View.VISIBLE) {
            attach.performClick();
            return true;
        }

        return false;
    }

    public long getConversationId() {
        return getArguments().getLong(ARG_CONVERSATION_ID);
    }

    public boolean isDragging() {
        return dragDismissFrameLayout.isDragging();
    }

    public void setItemDialog(AlertDialog dialog) {
        this.detailsChoiceDialog = dialog;
    }
}
