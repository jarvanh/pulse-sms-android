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

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v13.view.inputmethod.InputConnectionCompat;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.TooltipCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialcamera.MaterialCamera;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.sgottard.sofa.ContentFragment;
import com.turingtechnologies.materialscrollbar.DateAndTimeIndicator;
import com.turingtechnologies.materialscrollbar.MaterialScrollBar;
import com.turingtechnologies.materialscrollbar.TouchScrollBar;
import com.yalantis.ucrop.UCrop;

import net.ypresto.androidtranscoder.MediaTranscoder;
import net.ypresto.androidtranscoder.format.AndroidStandardFormatStrategy;
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.klinker.giphy.Giphy;
import xyz.klinker.messenger.BuildConfig;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.adapter.MessageListAdapter;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.FeatureFlags;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.MmsSettings;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Contact;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Draft;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.data.pojo.ConversationUpdateInfo;
import xyz.klinker.messenger.shared.data.pojo.KeyboardLayout;
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver;
import xyz.klinker.messenger.shared.service.NotificationService;
import xyz.klinker.messenger.shared.service.jobs.MarkAsSentJob;
import xyz.klinker.messenger.shared.shared_interfaces.IMessageListFragment;
import xyz.klinker.messenger.shared.util.AnimationUtils;
import xyz.klinker.messenger.shared.util.AudioWrapper;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.DensityUtil;
import xyz.klinker.messenger.shared.util.DualSimApplication;
import xyz.klinker.messenger.shared.util.DualSimUtils;
import xyz.klinker.messenger.shared.util.ImageUtils;
import xyz.klinker.messenger.shared.util.KeyboardLayoutHelper;
import xyz.klinker.messenger.shared.util.MessageCountHelper;
import xyz.klinker.messenger.shared.util.MessageListRefreshMonitor;
import xyz.klinker.messenger.shared.util.NotificationUtils;
import xyz.klinker.messenger.shared.util.PerformanceProfiler;
import xyz.klinker.messenger.shared.util.PermissionsUtils;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.SendUtils;
import xyz.klinker.messenger.shared.util.TvUtils;
import xyz.klinker.messenger.shared.util.VCardWriter;
import xyz.klinker.messenger.shared.util.listener.AttachContactListener;
import xyz.klinker.messenger.shared.util.listener.AudioRecordedListener;
import xyz.klinker.messenger.shared.util.listener.ImageSelectedListener;
import xyz.klinker.messenger.shared.util.listener.TextSelectedListener;
import xyz.klinker.messenger.utils.multi_select.MessageMultiSelectDelegate;
import xyz.klinker.messenger.view.AttachContactView;
import xyz.klinker.messenger.view.AttachImageView;
import xyz.klinker.messenger.view.AttachLocationView;
import xyz.klinker.messenger.view.ElasticDragDismissFrameLayout;
import xyz.klinker.messenger.view.ElasticDragDismissFrameLayout.ElasticDragDismissCallback;
import xyz.klinker.messenger.view.ImageKeyboardEditText;
import xyz.klinker.messenger.view.RecordAudioView;

import static android.app.Activity.RESULT_OK;

/**
 * Fragment for displaying messages for a certain conversation.
 */
public class MessageListFragment extends Fragment implements
        ImageSelectedListener, AudioRecordedListener, AttachContactListener, TextSelectedListener,
        ContentFragment, InputConnectionCompat.OnCommitContentListener, IMessageListFragment {

    public static final int MESSAGE_LIMIT = 8000;
    private boolean limitMessagesBasedOnPreviousSize = true;
    private int messageLoadedCount = -1;

    public static final String TAG = "MessageListFragment";
    public static final String ARG_TITLE = "title";
    public static final String ARG_PHONE_NUMBERS = "phone_numbers";
    public static final String ARG_COLOR = "color";
    public static final String ARG_COLOR_DARKER = "color_darker";
    public static final String ARG_COLOR_ACCENT = "color_accent";
    public static final String ARG_IS_GROUP = "is_group";
    public static final String ARG_CONVERSATION_ID = "conversation_id";
    public static final String ARG_MUTE_CONVERSATION = "mute_conversation";
    public static final String ARG_MESSAGE_TO_OPEN_ID = "message_to_open";
    public static final String ARG_READ = "read";
    public static final String ARG_IMAGE_URI = "image_uri";
    public static final String ARG_IS_ARCHIVED = "is_archived";
    public static final String ARG_LIMIT_MESSAGES = "limit_messages";

    private static final int PERMISSION_STORAGE_REQUEST = 1;
    private static final int PERMISSION_AUDIO_REQUEST = 2;
    private static final int RESULT_VIDEO_REQUEST = 3;
    private static final int RESULT_GIPHY_REQUEST = 4;
    private static final int PERMISSION_LOCATION_REQUEST = 5;
    private static final int RESULT_GALLERY_PICKER_REQUEST = 6;
    public static final int RESULT_CAPTURE_IMAGE_REQUEST = 7;

    private FragmentActivity activity;

    private DataSource source;
    private View appBarLayout;
    private Toolbar toolbar;
    private View sendBar;
    private EditText messageEntry;
    private MaterialScrollBar<TouchScrollBar> dragScrollBar;
    private View selectSim;
    private ImageButton attach;
    private FloatingActionButton send;
    private ProgressBar sendProgress;
    private TextView counter;
    private RecyclerView messageList;
    private LinearLayoutManager manager;
    private MessageListAdapter adapter;
    private ElasticDragDismissFrameLayout dragDismissFrameLayout;
    private ViewStub attachLayoutStub;
    private View attachLayout;
    private FrameLayout attachHolder;
    private LinearLayout attachButtonHolder;
    private View attachedImageHolder;
    private ImageView attachedImage;
    private TextView selectedImageCount;
    private View editImage;
    private View removeImage;
    private MessageListUpdatedReceiver updatedReceiver;
    private boolean dismissNotification = false;
    private boolean dismissOnStartup = false;
    private boolean textChanged = false;
    private List<Draft> drafts;
    private Map<String, Contact> contactMap;
    private Map<String, Contact> contactByNameMap;
    private boolean pullDrafts = true;

    private MessageMultiSelectDelegate multiSelect;
    private MessageListRefreshMonitor listRefreshMonitor = new MessageListRefreshMonitor();

    private Uri attachedUri;
    private String attachedMimeType;

    private List<String> selectedImageUris = new ArrayList<>();

    private AlertDialog detailsChoiceDialog;

    private CountDownTimer delayedTimer;
    private Handler delayedSendingHandler;

    private int extraMarginTop = 0;
    private int extraMarginLeft = 0;

    private boolean keyboardOpen = false;

    public static MessageListFragment newInstance(Conversation conversation) {
        return newInstance(conversation, -1);
    }

    public static MessageListFragment newInstance(Conversation conversation, long messageToOpenId) {
        return newInstance(conversation, messageToOpenId, true);
    }

    public static MessageListFragment newInstance(Conversation conversation, long messageToOpenId, boolean limitMessages) {
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
        args.putString(ARG_IMAGE_URI, conversation.imageUri);
        args.putBoolean(ARG_IS_ARCHIVED, conversation.archive);
        args.putBoolean(ARG_LIMIT_MESSAGES, limitMessages);

        if (messageToOpenId != -1) {
            args.putLong(ARG_MESSAGE_TO_OPEN_ID, messageToOpenId);
        }

        fragment.setArguments(args);
        return fragment;
    }

    public int getLayout() {
        return R.layout.fragment_message_list;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
        source = DataSource.INSTANCE;
        activity = getActivity();

        delayedSendingHandler = new Handler();
        multiSelect = new MessageMultiSelectDelegate(this);

        View view = inflater.inflate(getLayout(), parent, false);

        appBarLayout = view.findViewById(R.id.app_bar_layout);
        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        sendBar = view.findViewById(R.id.send_bar);
        messageEntry = (EditText) view.findViewById(R.id.message_entry);
        messageList = (RecyclerView) view.findViewById(R.id.message_list);
        dragScrollBar = (MaterialScrollBar) view.findViewById(R.id.drag_scrollbar);
        send = (FloatingActionButton) view.findViewById(R.id.send);
        sendProgress = (ProgressBar) view.findViewById(R.id.send_progress);

        if (!isAdded()) {
            return view;
        }

        initNonDeferredComponents(bundle);

        AnimationUtils.animateConversationPeripheralIn(appBarLayout);
        AnimationUtils.animateConversationPeripheralIn(sendBar);

        new Handler().postDelayed(() -> {
            if (!isAdded()) {
                return;
            }

            selectSim = view.findViewById(R.id.select_sim);
            attach = (ImageButton) view.findViewById(R.id.attach);
            counter = (TextView) view.findViewById(R.id.text_counter);
            attachLayoutStub = (ViewStub) view.findViewById(R.id.attach_stub);
            attachedImageHolder = view.findViewById(R.id.attached_image_holder);
            attachedImage = (ImageView) view.findViewById(R.id.attached_image);
            selectedImageCount = (TextView) view.findViewById(R.id.selected_images);
            removeImage = view.findViewById(R.id.remove_image);
            editImage = view.findViewById(R.id.edit_image);

            if (messageEntry instanceof ImageKeyboardEditText) {
                ((ImageKeyboardEditText) messageEntry).setCommitContentListener(this);
            }

            dragDismissFrameLayout = (ElasticDragDismissFrameLayout) view;
            dragDismissFrameLayout.addListener(new ElasticDragDismissCallback() {
                @Override
                public void onDragDismissed() {
                    dismissKeyboard();
                    activity.onBackPressed();
                }

                @Override
                public void onDrag(float elasticOffset, float elasticOffsetPixels,
                                   float rawOffset, float rawOffsetPixels) {

                }
            });

            dragDismissFrameLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                Rect r = new Rect();
                dragDismissFrameLayout.getWindowVisibleDisplayFrame(r);
                int screenHeight = dragDismissFrameLayout.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                keyboardOpen = keypadHeight > screenHeight * 0.15;
            });

            initSendbar();
            initAttachHolder();
            loadMessages();

            dismissNotification = true;
            dismissNotification();

            try {
                new DualSimApplication(selectSim).apply(getConversationId());
            } catch (Exception e) {
                // just in case
            }

        }, AnimationUtils.EXPAND_CONVERSATION_DURATION + 25);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updatedReceiver = new MessageListUpdatedReceiver(this);
        activity.registerReceiver(updatedReceiver,
                MessageListUpdatedReceiver.getIntentFilter());

        if (extraMarginLeft != 0 || extraMarginTop != 0) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
                    view.getLayoutParams();
            params.setMarginStart(extraMarginLeft);
            view.invalidate();
        }

        if (Settings.get(activity).rounderBubbles) {
            messageEntry.setBackground(activity.getResources().getDrawable(R.drawable.message_circle));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        NotificationService.CONVERSATION_ID_OPEN = getConversationId();
    }

    @Override
    public void onPause() {
        super.onPause();
        NotificationService.CONVERSATION_ID_OPEN = 0L;
    }
    @Override
    public void onStart() {
        super.onStart();
        dismissNotification = true;

        if (dismissOnStartup) {
            dismissNotification();
            dismissOnStartup = false;
        }

        new Handler().postDelayed(() -> {
            new Thread(() -> source.readConversation(activity, getConversationId())).start();
        }, AnimationUtils.EXPAND_CONVERSATION_DURATION + 50);
    }

    @Override
    public void onStop() {
        super.onStop();
        dismissNotification = false;

        new Handler().postDelayed(() -> {
            new Thread(() -> source.readConversation(activity, getConversationId())).start();
        }, AnimationUtils.EXPAND_CONVERSATION_DURATION + 50);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        createDrafts();

        if (sendProgress != null && sendProgress.getVisibility() == View.VISIBLE) {
            sendMessageOnFragmentClosed();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (updatedReceiver != null) {
            activity.unregisterReceiver(updatedReceiver);
            updatedReceiver = null;
        }

        createDrafts();

        multiSelect.clearActionMode();
    }

    public void setShouldPullDrafts(boolean pullDrafts) {
        this.pullDrafts = pullDrafts;
    }

    public void createDrafts() {
        if (sendProgress.getVisibility() != View.VISIBLE && messageEntry.getText() != null && messageEntry.getText().length() > 0 && textChanged) {
            if (drafts.size() > 0) {
                source.deleteDrafts(activity, getConversationId());
            }

            source.insertDraft(activity, getConversationId(),
                    messageEntry.getText().toString(), MimeType.TEXT_PLAIN);
        } else if (messageEntry.getText() != null && messageEntry.getText().length() == 0 && textChanged) {
            if (drafts.size() > 0) {
                source.deleteDrafts(activity, getConversationId());
            }
        }

        if (attachedUri != null && attachedMimeType != null) {
            source.insertDraft(activity, getConversationId(), attachedUri.toString(), attachedMimeType);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (adapter != null) {
            adapter.getMessages().close();
        }
    }

    private void initNonDeferredComponents(final Bundle bundle) {
        initToolbar();

        int accent = getArguments().getInt(ARG_COLOR_ACCENT);
        send.setBackgroundTintList(ColorStateList.valueOf(accent));
        messageEntry.setHighlightColor(accent);

        String firstName;
        if (getArguments() != null && getArguments().getString(ARG_TITLE) != null && !getArguments().getString(ARG_TITLE).isEmpty()) {
            firstName = getArguments().getString(ARG_TITLE).split(" ")[0];
        } else {
            firstName = "";
        }

        if (!getArguments().getBoolean(ARG_IS_GROUP) && !firstName.isEmpty()) {
            String hint = getResources().getString(R.string.type_message_to, firstName);
            messageEntry.setHint(hint);
        } else {
            messageEntry.setHint(R.string.type_message);
        }

        Settings settings = Settings.get(activity);
        if (settings.useGlobalThemeColor) {
            toolbar.setBackgroundColor(settings.mainColorSet.color);
            send.setBackgroundTintList(ColorStateList.valueOf(settings.mainColorSet.colorAccent));
            sendProgress.setProgressTintList(ColorStateList.valueOf(settings.mainColorSet.colorAccent));
            sendProgress.setProgressBackgroundTintList(ColorStateList.valueOf(settings.mainColorSet.colorAccent));
            messageEntry.setHighlightColor(settings.mainColorSet.colorAccent);
        }

        if (bundle == null) {
            initRecycler();
        }
    }

    private void initToolbar() {
        String name = getArguments().getString(ARG_TITLE);
        int color = getArguments().getInt(ARG_COLOR);
        int colorAccent = getArguments().getInt(ARG_COLOR_ACCENT);
        int colorDarker = getArguments().getInt(ARG_COLOR_DARKER);

        toolbar.setTitle(name);
        toolbar.setBackgroundColor(color);

        if (!getResources().getBoolean(R.bool.pin_drawer)) {
            // phone
            DrawerLayout drawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
            if (drawerLayout != null) {
                drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                    @Override
                    public void onDrawerSlide(View drawerView, float slideOffset) {
                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {
                    }

                    @Override
                    public void onDrawerStateChanged(int newState) {
                    }

                    @Override
                    public void onDrawerOpened(View drawerView) {
                        dismissKeyboard();
                    }
                });
            }

            toolbar.setNavigationIcon(R.drawable.ic_collapse);
            toolbar.setNavigationOnClickListener(view -> {
                dismissKeyboard();
                activity.onBackPressed();
            });
        } else {
            setNameAndDrawerColor(activity);
        }

        ColorUtils.adjustStatusBarColor(colorDarker, activity);

        new Handler().postDelayed(() -> {
            if (shouldLimitMessages()) {
                toolbar.inflateMenu(getArguments().getBoolean(ARG_IS_GROUP) ?
                        R.menu.fragment_messages_group : R.menu.fragment_messages);
            }

            try {
                MenuItem callItem = toolbar.getMenu().findItem(R.id.menu_call);
                ImageView image = new ImageView(activity);
                image.setImageResource(R.drawable.ic_call);
                image.setPaddingRelative(0, 0, DensityUtil.toDp(activity, 12), 0);
                callItem.setActionView(image);
                TooltipCompat.setTooltipText(callItem.getActionView(), getString(R.string.menu_call));

                image.setOnClickListener(view -> {
                    dismissKeyboard();
                    ((MessengerActivity) activity).menuItemClicked(R.id.menu_call);
                });

                image.setOnLongClickListener(view -> {
                    Toast.makeText(activity, R.string.menu_call, Toast.LENGTH_SHORT).show();
                    return true;
                });
            } catch (Exception e) {
                // rotation change probably
            }

            toolbar.setOnMenuItemClickListener(item -> {
                dismissKeyboard();
                ((MessengerActivity) activity).menuItemClicked(item.getItemId());

                return false;
            });

            if (!isAdded()) {
                return;
            }

            if (!getResources().getBoolean(R.bool.pin_drawer)) {
                setNameAndDrawerColor(activity);
            }

            ColorUtils.setCursorDrawableColor(messageEntry, colorAccent);
            ColorUtils.colorTextSelectionHandles(messageEntry, colorAccent);
        }, AnimationUtils.EXPAND_CONVERSATION_DURATION + 50);

        if (!TvUtils.hasTouchscreen(activity)) {
            appBarLayout.setVisibility(View.GONE);
        }
    }

    private void setNameAndDrawerColor(Activity activity) {
        String name = getArguments().getString(ARG_TITLE);
        String phoneNumber = PhoneNumberUtils.format(getArguments().getString(ARG_PHONE_NUMBERS));
        int colorDarker = getArguments().getInt(ARG_COLOR_DARKER);
        boolean isGroup = getArguments().getBoolean(ARG_IS_GROUP);
        String imageUri = getArguments().getString(ARG_IMAGE_URI);

        TextView nameView = (TextView) activity.findViewById(R.id.drawer_header_reveal_name);
        TextView phoneNumberView = (TextView) activity
                .findViewById(R.id.drawer_header_reveal_phone_number);
        ImageView image = (ImageView) activity.findViewById(R.id.drawer_header_reveal_image);

        // could be null when rotating the device
        if (nameView != null && name != null) {
            if (!name.equals(phoneNumber)) {
                nameView.setText(name);
            } else {
                messageEntry.setHint(R.string.type_message);
                nameView.setText("");
            }

            phoneNumberView.setText(phoneNumber);

            image.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
            if (imageUri != null) {
                Glide.with(activity)
                        .load(Uri.parse(imageUri))
                        .apply(new RequestOptions().signature(new ObjectKey(String.valueOf(System.currentTimeMillis()))))
                        .into(image);
            }

            ColorUtils.adjustDrawerColor(colorDarker, isGroup, activity);
        }

        NavigationView nav = (NavigationView) activity.findViewById(R.id.navigation_view);
        if (nav != null && getArguments().getBoolean(ARG_IS_ARCHIVED)) {
            MenuItem navItem = nav.getMenu().findItem(R.id.drawer_archive_conversation);
            MenuItem toolbarItem = toolbar.getMenu().findItem(R.id.menu_archive_conversation);

            if (navItem != null) {
                navItem.setTitle(R.string.menu_move_to_inbox);
            }

            if (toolbarItem != null) {
                toolbarItem.setTitle(R.string.menu_move_to_inbox);
            }
        }
    }

    private void initSendbar() {
        new KeyboardLayoutHelper(activity).applyLayout(messageEntry);
        messageEntry.setTextSize(Settings.get(activity).largeFont);

        messageEntry.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            boolean handled = false;

            if ((keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                    Settings.get(activity).keyboardLayout != KeyboardLayout.ENTER &&
                    keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) ||
                    actionId == EditorInfo.IME_ACTION_SEND) {
                requestPermissionThenSend();
                handled = true;
            }

            return handled;
        });

        messageEntry.setOnClickListener(view -> {
            if (attachLayout != null && attachLayout.getVisibility() == View.VISIBLE) {
                attach.setSoundEffectsEnabled(false);
                attach.performClick();
                attach.setSoundEffectsEnabled(true);
            }
        });

        final boolean sendOnEnter = Settings.get(activity).keyboardLayout == KeyboardLayout.SEND;
        messageEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                changeCounterText();
                if (sendOnEnter && charSequence.length() > 0) {
                    char lastKey = charSequence.charAt(charSequence.length() - 1);
                    if (lastKey == '\n') {
                        requestPermissionThenSend();
                    }
                }
            }

        });

        int accent = getArguments().getInt(ARG_COLOR_ACCENT);
        sendProgress.setProgressTintList(ColorStateList.valueOf(accent));
        sendProgress.setProgressBackgroundTintList(ColorStateList.valueOf(accent));
        sendProgress.setProgressTintMode(PorterDuff.Mode.SRC_IN);
        sendProgress.setProgressTintMode(PorterDuff.Mode.SRC_IN);
        send.setOnClickListener(view -> requestPermissionThenSend());

        String signature = Settings.get(activity).signature;
        if (signature != null && !signature.isEmpty()) {
            send.setOnLongClickListener(view -> {
                requestPermissionThenSend(true);
                return false;
            });
        }

        editImage.setBackgroundColor(accent);
        editImage.setOnClickListener(view -> {
            try {
                UCrop.Options options = new UCrop.Options();
                options.setToolbarColor(getArguments().getInt(ARG_COLOR));
                options.setStatusBarColor(getArguments().getInt(ARG_COLOR_DARKER));
                options.setActiveWidgetColor(getArguments().getInt(ARG_COLOR_ACCENT));
                options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
                options.setCompressionQuality(100);

                File destination = File.createTempFile("ucrop", "jpg", activity.getCacheDir());
                UCrop.of(attachedUri, Uri.fromFile(destination))
                        .withOptions(options)
                        .start(activity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        removeImage.setBackgroundColor(accent);
        removeImage.setOnClickListener(view -> {
            clearAttachedData();
            selectedImageUris.clear();
            selectedImageCount.setVisibility(View.GONE);

            if (attachLayout != null && attachLayout.getVisibility() == View.VISIBLE) {
                onBackPressed();
            }
        });

        selectedImageCount.setBackgroundColor(accent);
    }

    private void changeCounterText() {
        if (attachedUri == null && !getArguments().getBoolean(ARG_IS_GROUP) && !ignoreCounterText()) {
            Settings settings = Settings.get(activity);
            MmsSettings mmsSettings = MmsSettings.get(activity);
            String text = messageEntry.getText().toString();

            counter.setText(MessageCountHelper.getMessageCounterText(settings, mmsSettings, text));
        } else {
            counter.setText(/*R.string.mms_message*/ null);
        }
    }

    private boolean ignoreCounterText() {
        // they seem to have issues, where some dialog pops up, asking which SIM to send from
        // happens when the user is running LineageOS
        return !Account.INSTANCE.getPrimary() &&
                (Build.MODEL.equals("Nexus 9") || Build.MANUFACTURER.toLowerCase().equals("oneplus") ||
                        Build.MANUFACTURER.toLowerCase().equals("sony") || Build.MANUFACTURER.toLowerCase().equals("xiaomi") ||
                        Build.MANUFACTURER.toLowerCase().equals("samsung") || Build.MANUFACTURER.toLowerCase().equals("lge") ||
                        Build.MODEL.toLowerCase().contains("kindle"));
    }

    private void initAttachStub() {
        attachLayoutStub.inflate();
        View root = getView();

        if (root == null) {
            return;
        }

        attachLayout = root.findViewById(R.id.attach_layout);
        attachHolder = (FrameLayout) root.findViewById(R.id.attach_holder);
        attachButtonHolder = (LinearLayout) root.findViewById(R.id.attach_button_holder);
        ImageButton attachImage = (ImageButton) root.findViewById(R.id.attach_image);
        ImageButton captureImage = (ImageButton) root.findViewById(R.id.capture_image);
        ImageButton attachGif = (ImageButton) root.findViewById(R.id.attach_gif);
        ImageButton recordVideo = (ImageButton) root.findViewById(R.id.record_video);
        ImageButton recordAudio = (ImageButton) root.findViewById(R.id.record_audio);
        ImageButton attachLocation = (ImageButton) root.findViewById(R.id.attach_location);
        ImageButton attachContact = (ImageButton) root.findViewById(R.id.attach_contact);

        attachImage.setOnClickListener(view -> attachImage());
        captureImage.setOnClickListener(view -> captureImage());
        attachGif.setOnClickListener(view -> attachGif());
        recordVideo.setOnClickListener(view -> recordVideo());
        recordAudio.setOnClickListener(view -> recordAudio());
        attachLocation.setOnClickListener(view -> attachLocation());
        attachContact.setOnClickListener(view -> attachContact());

        boolean colorButtonsDark = false;
        Settings settings = Settings.get(activity);
        if (settings.useGlobalThemeColor) {
            attachButtonHolder.setBackgroundColor(settings.mainColorSet.color);
            if (!ColorUtils.isColorDark(settings.mainColorSet.color)) {
                colorButtonsDark = true;
            }
        } else {
            attachButtonHolder.setBackgroundColor(getArguments().getInt(ARG_COLOR));
            if (!ColorUtils.isColorDark(getArguments().getInt(ARG_COLOR))) {
                colorButtonsDark = true;
            }
        }

        if (colorButtonsDark) {
            ColorStateList list = ColorStateList.valueOf(getResources().getColor(R.color.lightToolbarTextColor));
            attachImage.setImageTintList(list);
            captureImage.setImageTintList(list);
            attachGif.setImageTintList(list);
            recordVideo.setImageTintList(list);
            recordAudio.setImageTintList(list);
            attachLocation.setImageTintList(list);
            attachContact.setImageTintList(list);
        }
    }

    private void initAttachHolder() {
        if (!TvUtils.hasTouchscreen(activity)) {
            attach.setVisibility(View.GONE);
            send.setNextFocusDownId(R.id.message_entry);
        }

        attach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (attachLayout == null) {
                    initAttachStub();
                }

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
                    attachImage(true);
                    attachLayout.setVisibility(View.VISIBLE);
                    animator = ValueAnimator.ofInt(0,
                            getResources().getDimensionPixelSize(R.dimen.attach_menu_height));
                }

                final ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) attachLayout.getLayoutParams();

                animator.addUpdateListener(valueAnimator -> {
                    params.height = (Integer) valueAnimator.getAnimatedValue();
                    attachLayout.requestLayout();
                });

                animator.setDuration(200);
                animator.start();
            }
        });
    }

    private void initRecycler() {
        ColorUtils.changeRecyclerOverscrollColors(messageList, getArguments().getInt(ARG_COLOR));

        manager = new LinearLayoutManager(activity);
        manager.setStackFromEnd(true);
        messageList.setLayoutManager(manager);
        adapter = null;

        final Settings settings = Settings.get(activity);
        dragScrollBar.setIndicator(new DateAndTimeIndicator(
                                activity, true, true, true, false),
                        true)
                .setHandleColour(settings.useGlobalThemeColor ?
                        settings.mainColorSet.color : getArguments().getInt(ARG_COLOR))
                .setFastScrollSnapPercent(.05f);

        messageList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = manager.getChildCount();
                int totalItemCount = manager.getItemCount();
                int pastVisibleItems = manager.findFirstVisibleItemPosition();
                if (pastVisibleItems + visibleItemCount >= totalItemCount && adapter != null && adapter.snackbar != null) {
                    adapter.snackbar.dismiss();
                }
            }
        });
    }

    private void dismissNotification() {
        try {
            if (dismissNotification && notificationActive()) {
                NotificationManagerCompat.from(activity)
                        .cancel((int) getConversationId());

                ApiUtils.INSTANCE.dismissNotification(Account.INSTANCE.getAccountId(),
                        Account.INSTANCE.getDeviceId(),
                        getConversationId());

                NotificationUtils.cancelGroupedNotificationWithNoContent(activity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean notificationActive() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        } else {
            NotificationManager manager = (NotificationManager) activity
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            StatusBarNotification[] notifications = manager.getActiveNotifications();

            int notificationId = (int) getConversationId();
            for (StatusBarNotification notification : notifications) {
                if (notification.getId() == notificationId) {
                    return true;
                }
            }

            return false;
        }
    }

    public void loadMessages() {
        loadMessages(false);
    }

    public void loadMessages(boolean addedNewMessage) {
        final Handler handler = new Handler();
        new Thread(() -> {
            PerformanceProfiler.INSTANCE.logEvent("loading messages");

            long conversationId = getConversationId();

            try {
                listRefreshMonitor.incrementRefreshThreadsCount();
                long startTime = System.currentTimeMillis();
                drafts = source.getDrafts(activity, conversationId);

                final Cursor cursor;
                if (shouldLimitMessages() && limitMessagesBasedOnPreviousSize) {
                    // weird logic with the counts for this. If we just load the MESSAGE_LIMIT each time,
                    // then the adapter gets screwed up and can display the wrong messages, since recycler views
                    // are meant to be "smart" about managing state.
                    // So, if we send a message, or a message is received, we should increment the number of messages
                    // that we are reading from the database, to account for this.

                    cursor = source.getMessageCursorWithLimit(activity, conversationId,
                            messageLoadedCount == -1 ? MESSAGE_LIMIT :
                                    addedNewMessage ? messageLoadedCount + 1 : messageLoadedCount
                    );

                    if (cursor.getCount() < MESSAGE_LIMIT) {
                        // When the conversations are small enough, then we shouldn't need to do this
                        // this is just a slight cleanup to remove the extra size check that happens in the
                        // above data load. If it isn't necessary, then we shouldn't do it
                        limitMessagesBasedOnPreviousSize = false;
                    }
                } else {
                    cursor = source.getMessages(activity, conversationId);
                }

                messageLoadedCount = cursor.getCount();

                final String numbers = getArguments().getString(ARG_PHONE_NUMBERS);
                final String title = getArguments().getString(ARG_TITLE);

                if (contactMap == null || contactByNameMap == null) {
                    final List<Contact> contacts = source.getContacts(activity, numbers);
                    final List<Contact> contactsByName = source.getContactsByNames(activity, title);
                    contactMap = fillMapByNumber(numbers, contacts);
                    contactByNameMap = fillMapByName(title, contactsByName);
                }

                final int position = findMessagePositionFromId(cursor);

                PerformanceProfiler.INSTANCE.logEvent("finished loading messages");

                listRefreshMonitor.decrementRefreshThreadsCount();
                if (listRefreshMonitor.shouldLoadMessagesToTheUi()) {
                    listRefreshMonitor.resetRunningThreadCount();

                    handler.post(() -> {
                        setMessages(cursor, contactMap, contactByNameMap);
                        textChanged = false;

                        if (pullDrafts) {
                            setDrafts(drafts);
                        } else {
                            pullDrafts = true;
                        }

                        if (position != -1) {
                            messageList.scrollToPosition(position);
                        }

                        messageEntry.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                            }

                            @Override
                            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                            }

                            @Override
                            public void afterTextChanged(Editable editable) {
                                textChanged = true;
                            }
                        });
                    });
                }

                if (!getArguments().getBoolean(ARG_IS_GROUP)) {
                    String number = getArguments().getString(ARG_PHONE_NUMBERS);
                    final String name = ContactUtils.findContactNames(number, activity);
                    String photoUri = ContactUtils.findImageUri(number, activity);
                    if (photoUri != null && !photoUri.isEmpty()) {
                        photoUri += "/photo";
                    }

                    if (!name.equals(getArguments().getString(ARG_TITLE)) &&
                            !PhoneNumberUtils.checkEquality(name, number)) {
                        Log.v(TAG, "contact name and conversation name do not match, updating");
                        source.updateConversationTitle(activity,
                                getArguments().getLong(ARG_CONVERSATION_ID), name);

                        ConversationListFragment fragment = (ConversationListFragment) activity
                                .getSupportFragmentManager().findFragmentById(R.id.conversation_list_container);

                        if (fragment != null) {
                            fragment.setNewConversationTitle(name);
                        }

                        handler.post(() -> toolbar.setTitle(name));
                    }

                    String originalImage = getArguments().getString(ARG_IMAGE_URI);
                    if ((photoUri != null && (!photoUri.equals(originalImage)) || originalImage == null || originalImage.isEmpty())) {
                        source.updateConversationImage(activity, getArguments().getLong(ARG_CONVERSATION_ID), photoUri);
                    }
                }

                if (NotificationService.CONVERSATION_ID_OPEN == getConversationId()) {
                    Thread.sleep(1000);

                    // this could happen in the background, we don't want to dismiss that then!
                    dismissNotification();
                    dismissOnStartup = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private Map<String, Contact> fillMapByName(String title, List<Contact> contacts) {
        try {
            return title != null && title.contains(", ") ? ContactUtils.getMessageFromMappingByTitle(
                    title, contacts
            ) : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private Map<String, Contact> fillMapByNumber(String numbers, List<Contact> contacts) {
        try {
            return ContactUtils.getMessageFromMapping(
                    numbers, contacts, source, activity
            );
        } catch (Exception e) {
            return new HashMap<>();
        }
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

    private void setMessages(Cursor messages, Map<String, Contact> contactMap, Map<String, Contact> contactMapByName) {
        if (adapter != null) {
            adapter.addMessage(messageList, messages);
        } else {
            adapter = new MessageListAdapter(messages, getArguments().getInt(ARG_COLOR),
                    Settings.get(activity).useGlobalThemeColor ?
                            Settings.get(activity).mainColorSet.colorAccent :
                            getArguments().getInt(ARG_COLOR_ACCENT),
                    getArguments().getBoolean(ARG_IS_GROUP), manager, this);
            adapter.setFromColorMapper(contactMap, contactMapByName);

            if (messageList != null) {
                messageList.setAdapter(adapter);
                messageList.animate().withLayer()
                        .alpha(1f).setDuration(100).setStartDelay(0).setListener(null);
            }
        }
    }

    private void setDrafts(List<Draft> drafts) {
        for (Draft draft : drafts) {
            if (draft.mimeType.equals(MimeType.TEXT_PLAIN)) {
                textChanged = true;
                messageEntry.setText(draft.data);
                messageEntry.setSelection(messageEntry.getText().length());
            } else if (MimeType.isStaticImage(draft.mimeType)) {
                attachImage(Uri.parse(draft.data));
            } else if (draft.mimeType.equals(MimeType.IMAGE_GIF)) {
                attachImage(Uri.parse(draft.data));
                attachedMimeType = draft.mimeType;
                editImage.setVisibility(View.GONE);
            } else if (draft.mimeType.contains("audio/")) {
                attachAudio(Uri.parse(draft.data));
                attachedMimeType = draft.mimeType;
                editImage.setVisibility(View.GONE);
            } else if (draft.mimeType.contains("video/")) {
                attachImage(Uri.parse(draft.data));
                attachedMimeType = draft.mimeType;
                editImage.setVisibility(View.GONE);
            } else if (draft.mimeType.equals(MimeType.TEXT_VCARD)) {
                attachContact(Uri.parse(draft.data));
                attachedMimeType = draft.mimeType;
                editImage.setVisibility(View.GONE);
            }
        }
    }

    public void resendMessage(long originalMessageId, final String text) {
        source.deleteMessage(activity, originalMessageId);
        messageLoadedCount--;

        loadMessages(false);

        new Handler().postDelayed(() -> {
            messageEntry.setText(text);
            requestPermissionThenSend();
        }, 300);
    }

    private void requestPermissionThenSend() {
        requestPermissionThenSend(false);
    }

    private void requestPermissionThenSend(final boolean forceNoSignature) {
        // finding the message and URIs is also done in the onBackPressed method.
        final String message = messageEntry.getText().toString().trim();
        final List<Uri> uris = new ArrayList<>();

        if (selectedImageUris.size() > 0) {
            for (String uri : selectedImageUris) {
                uris.add(Uri.parse(uri));
            }
        } else if (attachedUri != null) {
            uris.add(attachedUri);
        }

        if (PermissionsUtils.checkRequestMainPermissions(activity)) {
            PermissionsUtils.startMainPermissionRequest(activity);
        } else if (Account.INSTANCE.getPrimary() &&
                !PermissionsUtils.isDefaultSmsApp(activity)) {
            PermissionsUtils.setDefaultSmsApp(activity);
        } else if (message.length() > 0 || uris.size() > 0) {
            Settings settings = Settings.get(activity);
            if (settings.delayedSendingTimeout != 0) {
                changeDelayedSendingComponents(true);
            }

            delayedSendingHandler.postDelayed(() -> sendMessage(uris, forceNoSignature), settings.delayedSendingTimeout);
        }
    }

    private void changeDelayedSendingComponents(boolean start) {
        delayedSendingHandler.removeCallbacksAndMessages(null);
        if (delayedTimer != null) {
            delayedTimer.cancel();
        }

        if (!start) {
            sendProgress.setProgress(0);
            sendProgress.setVisibility(View.INVISIBLE);
            send.setImageResource(R.drawable.ic_send);
            send.setOnClickListener((view) -> requestPermissionThenSend());
        } else {
            sendProgress.setIndeterminate(false);
            sendProgress.setVisibility(View.VISIBLE);
            send.setImageResource(R.drawable.ic_close);
            send.setOnClickListener((view) -> changeDelayedSendingComponents(false));

            final Settings settings = Settings.get(activity);
            sendProgress.setMax((int) settings.delayedSendingTimeout / 10);

            delayedTimer = new CountDownTimer(settings.delayedSendingTimeout, 10) {
                @Override
                public void onFinish() {
                }

                @Override
                public void onTick(long millisUntilFinished) {
                    sendProgress.setProgress((int) (settings.delayedSendingTimeout - millisUntilFinished) / 10);
                }
            }.start();
        }
    }

    private void sendMessage(final List<Uri> uris) {
        sendMessage(uris,false);
    }

    private void sendMessage(final List<Uri> uris, final boolean forceNoSignature) {
        changeDelayedSendingComponents(false);

        final String message = messageEntry.getText().toString().trim();
        final String mimeType = attachedMimeType != null ?
                attachedMimeType : MimeType.TEXT_PLAIN;

        if ((message.length() > 0 || uris.size() > 0) && activity != null) {
            Conversation conversation = source.getConversation(activity, getConversationId());

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
            m.simPhoneNumber = conversation != null && conversation.simSubscriptionId != null ?
                    DualSimUtils.get(activity).getPhoneNumberFromSimSubscription(conversation.simSubscriptionId) : null;
            m.sentDeviceId = Account.INSTANCE.exists() ? Long.parseLong(Account.INSTANCE.getDeviceId()) : -1L;

            if (adapter != null && adapter.getItemViewType(0) == Message.TYPE_INFO) {
                source.deleteMessage(activity, adapter.getItemId(0));
            }

            clearAttachedData();
            selectedImageUris.clear();
            selectedImageCount.setVisibility(View.GONE);
            source.deleteDrafts(activity, getConversationId());
            messageEntry.setText(null);

            if (activity != null) {
                Fragment fragment = activity
                        .getSupportFragmentManager().findFragmentById(R.id.conversation_list_container);

                if (fragment != null && fragment instanceof ConversationListFragment) {
                    ((ConversationListFragment) fragment).notifyOfSentMessage(m);
                }
            }

            boolean loadMessages = false;

            if (message.length() != 0) {
                source.insertMessage(activity, m, m.conversationId);
                loadMessages = true;
            }

            if (uris.size() > 0) {
                m.data = uris.get(0).toString();
                m.mimeType = mimeType;

                if (m.id != 0) {
                    m.id = 0;
                }

                m.id = source.insertMessage(activity, m, m.conversationId, true);

                loadMessages = true;
            }

            new Thread(() -> {
                Uri imageUri = new SendUtils(conversation != null ? conversation.simSubscriptionId : null)
                        .setForceNoSignature(forceNoSignature)
                        .send(activity, message, getArguments().getString(ARG_PHONE_NUMBERS),
                                uris.size() > 0 ? uris.get(0) : null, mimeType);
                MarkAsSentJob.Companion.scheduleNextRun(activity, m.id);

                if (imageUri != null && activity != null) {
                    source.updateMessageData(activity, m.id, imageUri.toString());
                }
            }).start();

            if (uris.size() > 1) {
                for (int i = 1; i < uris.size(); i++) {
                    final Uri sendUri = uris.get(i);
                    m.data = sendUri.toString();
                    m.mimeType = mimeType;
                    m.id = 0;

                    m.id = source.insertMessage(activity, m, m.conversationId, true);

                    new Thread(() -> {
                        Uri imageUri = new SendUtils(conversation != null ? conversation.simSubscriptionId : null)
                                .setForceNoSignature(forceNoSignature)
                                .send(activity, message, getArguments().getString(ARG_PHONE_NUMBERS),
                                        sendUri, mimeType);
                        MarkAsSentJob.Companion.scheduleNextRun(activity, m.id);

                        if (imageUri != null && activity != null) {
                            source.updateMessageData(activity, m.id, imageUri.toString());
                        }
                    }).start();
                }

                loadMessages = true;
            }

            if (loadMessages) {
                loadMessages(true);
            }

            new AudioWrapper(activity, R.raw.message_ping).play();

            if (notificationActive()) {
                NotificationManagerCompat.from(activity)
                        .cancel((int) getConversationId());
                NotificationUtils.cancelGroupedNotificationWithNoContent(activity);
            }
        }
    }

    private void attachImage() {
        attachImage(false);
    }

    private void attachImage(boolean alwaysOpen) {
        if (!alwaysOpen && getBoldedAttachHolderPosition() == 0) {
            return;
        }

        try {
            prepareAttachHolder(0);
            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    attachHolder != null) {
                attachHolder.addView(new AttachImageView(activity, this,
                        Settings.get(activity).useGlobalThemeColor ?
                                Settings.get(activity).mainColorSet.color :
                                getArguments().getInt(ARG_COLOR)));
            } else {
                attachPermissionRequest(PERMISSION_STORAGE_REQUEST,
                        Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } catch (NullPointerException e) {

        }
    }

    private void captureImage() {
        if (getBoldedAttachHolderPosition() == 1) {
            return;
        }

        prepareAttachHolder(1);

        Camera2BasicFragment fragment = Camera2BasicFragment.newInstance();
        getFragmentManager().beginTransaction().add(R.id.attach_holder, fragment).commit();
        fragment.attachImageSelectedListener(this);
    }

    private void attachGif() {
        if (getBoldedAttachHolderPosition() == 2) {
            return;
        }

        prepareAttachHolder(2);
        new Giphy.Builder(activity, BuildConfig.GIPHY_API_KEY)
                .maxFileSize(MmsSettings.get(activity).maxImageSize)
                .start();
    }

    private void recordVideo() {
        if (getBoldedAttachHolderPosition() == 3) {
            return;
        }

        prepareAttachHolder(3);

        MaterialCamera camera = new MaterialCamera(activity)
                .saveDir(activity.getFilesDir().getPath())
                .qualityProfile(MaterialCamera.QUALITY_LOW)
                .maxAllowedFileSize(MmsSettings.get(activity).maxImageSize)
                .allowRetry(false)
                .autoSubmit(true)
                .showPortraitWarning(false);

        if (Settings.get(activity).useGlobalThemeColor) {
            camera.primaryColor(Settings.get(activity).mainColorSet.color);
        } else {
            camera.primaryColor(getArguments().getInt(ARG_COLOR));
        }

        camera.start(RESULT_VIDEO_REQUEST);
    }

    private void recordAudio() {
        recordAudio(false);
    }

    private void recordAudio(boolean alwaysOpen) {
        if (!alwaysOpen && getBoldedAttachHolderPosition() == 4) {
            return;
        }

        prepareAttachHolder(4);
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            attachHolder.addView(new RecordAudioView(activity, this,
                    Settings.get(activity).useGlobalThemeColor ?
                            Settings.get(activity).mainColorSet.colorAccent :
                            getArguments().getInt(ARG_COLOR_ACCENT)));
        } else {
            attachPermissionRequest(PERMISSION_AUDIO_REQUEST,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO);
        }
    }

    private void attachLocation() {
        attachLocation(false);
    }

    private void attachLocation(boolean alwaysOpen) {
        if (!alwaysOpen && getBoldedAttachHolderPosition() == 5) {
            return;
        }

        prepareAttachHolder(5);
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            attachHolder.addView(new AttachLocationView(activity, this, this,
                    Settings.get(activity).useGlobalThemeColor ?
                            Settings.get(activity).mainColorSet.colorAccent :
                            getArguments().getInt(ARG_COLOR_ACCENT)));
        } else {
            attachPermissionRequest(PERMISSION_LOCATION_REQUEST,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void attachContact() {
        if (getBoldedAttachHolderPosition() == 6) {
            return;
        }

        prepareAttachHolder(6);
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            attachHolder.addView(new AttachContactView(activity, this,
                    Settings.get(activity).useGlobalThemeColor ?
                            Settings.get(activity).mainColorSet.color :
                            getArguments().getInt(ARG_COLOR)));
        } else {
            attachPermissionRequest(PERMISSION_AUDIO_REQUEST,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void attachPermissionRequest(final int permissionRequestCode, final String... permissions) {
        LayoutInflater.from(activity).inflate(R.layout.permission_request, attachHolder, true);
        Button request = (Button) attachHolder.findViewById(R.id.permission_needed);
        request.setOnClickListener(view -> requestPermissions(permissions, permissionRequestCode));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        try {
            if (requestCode == PERMISSION_STORAGE_REQUEST) {
                attachImage(true);
            } else if (requestCode == PERMISSION_AUDIO_REQUEST) {
                recordAudio(true);
            } else if (requestCode == PERMISSION_LOCATION_REQUEST) {
                attachLocation(true);
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            attachImage(UCrop.getOutput(data));

            selectedImageUris.clear();
            selectedImageCount.setVisibility(View.GONE);
        } else if (requestCode == RESULT_VIDEO_REQUEST) {
            onBackPressed();

            if (resultCode == RESULT_OK) {
                Log.v("video result", "saved to " + data.getDataString());
                attachImage(data.getData());
                attachedMimeType = MimeType.VIDEO_MP4;
                editImage.setVisibility(View.GONE);

                selectedImageUris.clear();
                selectedImageCount.setVisibility(View.GONE);
            } else if (data != null) {
                Exception e = (Exception) data.getSerializableExtra(MaterialCamera.ERROR_EXTRA);
                e.printStackTrace();
                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == Giphy.REQUEST_GIPHY) {
            onBackPressed();

            if (resultCode == RESULT_OK) {
                Log.v("gif result", "saved to " + data.getDataString());
                attachImage(data.getData());
                attachedMimeType = MimeType.IMAGE_GIF;
                editImage.setVisibility(View.GONE);

                selectedImageUris.clear();
                selectedImageCount.setVisibility(View.GONE);
            }
        } else if (requestCode == RESULT_GALLERY_PICKER_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            onBackPressed();
            Uri uri = data.getData();
            String uriString = data.getDataString();
            String mimeType = MimeType.IMAGE_JPEG;
            if (uriString.contains("content://")) {
                mimeType = activity.getContentResolver().getType(uri);
            }

            attachImage(uri);
            if (mimeType != null && mimeType.equals(MimeType.IMAGE_GIF)) {
                attachedMimeType = MimeType.IMAGE_GIF;
                editImage.setVisibility(View.GONE);
            }

            selectedImageUris.clear();
            selectedImageCount.setVisibility(View.GONE);
        } else if (requestCode == RESULT_CAPTURE_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri uri = ImageUtils.getUriForLatestPhoto(activity);
            onBackPressed();
            attachImage(uri);

            selectedImageUris.clear();
            selectedImageCount.setVisibility(View.GONE);
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

    private int getBoldedAttachHolderPosition() {
        for (int i = 0; i < attachButtonHolder.getChildCount(); i++) {
            if (attachButtonHolder.getChildAt(i).getAlpha() == 1.0f) {
                return i;
            }
        }

        return -1;
    }

    private void clearAttachedData() {
        if (activity != null) {
            source.deleteDrafts(activity, getConversationId());
        }

        attachedImageHolder.setVisibility(View.GONE);
        attachedImage.setImageDrawable(null);
        attachedUri = null;
        attachedMimeType = null;
        changeCounterText();
    }

    private void dismissKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager)
                    activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(messageEntry.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isCurrentlySelected(Uri uri, String mimeType) {
        return selectedImageUris.contains(uri.toString()) ||
                (attachedUri != null && uri.toString().equals(attachedUri.toString()));
    }

    @Override
    public void onImageSelected(Uri uri, String mimeType) {
        if (selectedImageUris.size() == 0) {
            // auto close the attach view after selecting the first image
            onBackPressed();
        }

        if (MimeType.isStaticImage(mimeType)) {
            if (!selectedImageUris.contains(uri.toString())) {
                attachImage(uri);
                selectedImageUris.add(uri.toString());
            } else {
                selectedImageUris.remove(uri.toString());
                if (selectedImageUris.size() > 0) {
                    attachImage(Uri.parse(selectedImageUris.get(0)));
                }
            }

            if (selectedImageUris.size() == 0) {
                clearAttachedData();
                selectedImageUris.clear();
                selectedImageCount.setVisibility(View.GONE);
                editImage.setVisibility(View.VISIBLE);
            } else if (selectedImageUris.size() > 1) {
                selectedImageCount.setVisibility(View.VISIBLE);
                selectedImageCount.setText(String.valueOf(selectedImageUris.size()));
                editImage.setVisibility(View.GONE);
            } else {
                selectedImageCount.setVisibility(View.GONE);
                editImage.setVisibility(View.VISIBLE);
            }
        } else if (MimeType.isVideo(mimeType)) {
            startVideoEncoding(uri);
            selectedImageUris.clear();
            selectedImageCount.setVisibility(View.GONE);

            if (attachHolder.getVisibility() == View.VISIBLE) {
                attach.performClick();
            }
        } else if (mimeType.equals(MimeType.IMAGE_GIF)) {
            attachImage(uri);
            attachedMimeType = MimeType.IMAGE_GIF;
            editImage.setVisibility(View.GONE);
            selectedImageUris.clear();
            selectedImageCount.setVisibility(View.GONE);

            if (attachHolder.getVisibility() == View.VISIBLE) {
                attach.performClick();
            }
        }
    }

    @Override
    public void onGalleryPicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_GALLERY_PICKER_REQUEST);
    }

    @Override
    public void onRecorded(Uri uri) {
        onBackPressed();
        attachAudio(uri);
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onContactAttached(String firstName, String lastName, String phone) {
        onBackPressed();

        if (FeatureFlags.get(getActivity()).V_2_6_0) {
            new AlertDialog.Builder(getActivity())
                    .setItems(R.array.attach_contact_options, (dialogInterface, i) -> {
                        switch (i) {
                            case 0:
                                try {
                                    Uri contactFile = VCardWriter.writeContactCard(activity, firstName, lastName, phone);
                                    attachContact(contactFile);
                                } catch (Exception e) { }
                                break;
                            case 1:
                                messageEntry.setText(firstName + " " + lastName + ": " + phone);
                                break;
                        }
                    }).show();
        } else {
            try {
                Uri contactFile = VCardWriter.writeContactCard(activity, firstName, lastName, phone);
                attachContact(contactFile);
            } catch (Exception e) { }
        }
    }

    @Override
    public void onTextSelected(String text) {
        messageEntry.setText(text);
    }

    private void attachImage(Uri uri) {
        if (editImage == null) {
            // this happens when opening the full screen capture image intent
            // then rotating from landscape to portrait
            // the view has not been initialized by the time we are trying to deliver the results, so delay it.
            new Handler().postDelayed(() -> attachImage(uri), 500);
            return;
        }

        editImage.setVisibility(View.VISIBLE);

        clearAttachedData();
        attachedUri = uri;
        attachedMimeType = MimeType.IMAGE_JPG;

        attachedImageHolder.setVisibility(View.VISIBLE);

        try {
            if (activity != null) {
                Glide.with(activity)
                        .load(uri)
                        .apply(new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.DATA)
                                .placeholder(R.drawable.ic_image_sending))
                        .into(attachedImage);
                changeCounterText();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void attachAudio(Uri uri) {
        clearAttachedData();
        attachedUri = uri;
        attachedMimeType = MimeType.AUDIO_MP4;
        editImage.setVisibility(View.GONE);

        attachedImageHolder.setVisibility(View.VISIBLE);
        attachedImage.setImageResource(R.drawable.ic_audio_sent);
        changeCounterText();
    }

    private void attachContact(Uri uri) {
        clearAttachedData();
        attachedUri = uri;
        attachedMimeType = MimeType.TEXT_VCARD;
        editImage.setVisibility(View.GONE);

        attachedImageHolder.setVisibility(View.VISIBLE);
        attachedImage.setImageResource(R.drawable.ic_contacts);
        attachedImage.setImageTintList(ColorStateList.valueOf(Color.BLACK));
        changeCounterText();
    }

    public boolean onBackPressed() {
        dismissDetailsChoiceDialog();

        if (attachLayout != null && attachLayout.getVisibility() == View.VISIBLE) {
            attach.setSoundEffectsEnabled(false);
            attach.performClick();
            attach.setSoundEffectsEnabled(true);
            return true;
        } else if (sendProgress != null && sendProgress.getVisibility() == View.VISIBLE) {
            sendMessageOnFragmentClosed();
        }

        if (updatedReceiver != null) {
            activity.unregisterReceiver(updatedReceiver);
            updatedReceiver = null;
        }

        return false;
    }

    private void sendMessageOnFragmentClosed() {
        sendProgress.setVisibility(View.GONE);
        delayedSendingHandler.removeCallbacksAndMessages(null);

        final List<Uri> uris = new ArrayList<>();

        if (selectedImageUris.size() > 0) {
            for (String uri : selectedImageUris) {
                uris.add(Uri.parse(uri));
            }
        } else if (attachedUri != null) {
            uris.add(attachedUri);
        }

        sendMessage(uris);
        messageEntry.setText("");
    }

    public long getConversationId() {
        return getArguments().getLong(ARG_CONVERSATION_ID);
    }

    private boolean shouldLimitMessages() {
        return getArguments().getBoolean(ARG_LIMIT_MESSAGES, true);
    }

    public boolean isDragging() {
        return dragDismissFrameLayout.isDragging();
    }

    public void setDetailsChoiceDialog(AlertDialog dialog) {
        this.detailsChoiceDialog = dialog;
    }

    private void dismissDetailsChoiceDialog() {
        if (detailsChoiceDialog != null && detailsChoiceDialog.isShowing()) {
            detailsChoiceDialog.dismiss();
            detailsChoiceDialog = null;
        }
    }

    public boolean isRecyclerScrolling() {
        return messageList != null && messageList.getScrollState() != RecyclerView.SCROLL_STATE_IDLE;
    }

    @Override
    public boolean isScrolling() {
        return false;
    }

    @Override
    public View getFocusRootView() {
        try {
            return messageList.getChildAt(messageList.getChildCount() - 1);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public void setExtraMargin(int marginTop, int marginLeft) {
        this.extraMarginTop = marginTop;
        this.extraMarginLeft = marginLeft;
    }

    public void setConversationUpdateInfo(String newMessage) {
        Fragment fragment = activity.getSupportFragmentManager()
                .findFragmentById(R.id.conversation_list_container);

        if (fragment != null && newMessage != null && fragment instanceof ConversationListFragment) {
            ((ConversationListFragment) fragment).setConversationUpdateInfo(new ConversationUpdateInfo(
                    getConversationId(), newMessage, true));
        }
    }

    public void setDismissOnStartup() {
        this.dismissOnStartup = true;
    }

    public void startVideoEncoding(final Uri uri) {
        startVideoEncoding(uri, AndroidStandardFormatStrategy.Encoding.SD_LOW);
    }

    public void startVideoEncoding(final Uri uri, AndroidStandardFormatStrategy.Encoding encoding) {
        File original = new File(uri.getPath());
        if (original.length() < MmsSettings.get(activity).maxImageSize) {
            attachImage(uri);
            attachedMimeType = MimeType.VIDEO_MP4;
            editImage.setVisibility(View.GONE);
        } else {
            final File file;
            try {
                File outputDir = new File(activity.getExternalFilesDir(null), "outputs");
                boolean mkdir = outputDir.mkdir();
                file = File.createTempFile("transcode_video", ".mp4", outputDir);
            } catch (IOException e) {
                Toast.makeText(activity, "Failed to create temporary file.", Toast.LENGTH_LONG).show();
                return;
            }

            ContentResolver resolver = activity.getContentResolver();
            final ParcelFileDescriptor parcelFileDescriptor;
            try {
                parcelFileDescriptor = resolver.openFileDescriptor(uri, "r");
            } catch (FileNotFoundException e) {
                Toast.makeText(activity, "File not found.", Toast.LENGTH_LONG).show();
                return;
            }

            final ProgressDialog progressDialog = new ProgressDialog(activity);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(activity.getString(R.string.preparing_video));

            if (parcelFileDescriptor == null) {
                return;
            }

            final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            MediaTranscoder.Listener listener = new MediaTranscoder.Listener() {
                @Override
                public void onTranscodeCanceled() {
                }

                @Override
                public void onTranscodeProgress(double progress) {
                }

                @Override
                public void onTranscodeFailed(Exception exception) {
                    exception.printStackTrace();
                    Toast.makeText(activity,
                            "Failed to process video for sending: " + exception.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    try {
                        progressDialog.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onTranscodeCompleted() {
                    attachImage(ImageUtils.createContentUri(activity, file));
                    attachedMimeType = MimeType.VIDEO_MP4;
                    editImage.setVisibility(View.GONE);

                    try {
                        progressDialog.cancel();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            progressDialog.show();
            MediaTranscoder.getInstance().transcodeVideo(fileDescriptor, file.getAbsolutePath(),
                    MediaFormatStrategyPresets.createStandardFormatStrategy(encoding), listener);
        }
    }

    @Override
    public boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags, Bundle opts) {
        String mime = inputContentInfo.getDescription().getMimeType(0);

        if (mime.equals(MimeType.IMAGE_GIF)) {
            attachImage(inputContentInfo.getContentUri());
            attachedMimeType = MimeType.IMAGE_GIF;
            editImage.setVisibility(View.GONE);
        } else if (mime.contains("image/")) {
            attachImage(inputContentInfo.getContentUri());
            attachedMimeType = MimeType.IMAGE_PNG;
        } else if (mime.contains(MimeType.VIDEO_MP4)) {
            attachImage(inputContentInfo.getContentUri());
            attachedMimeType = MimeType.VIDEO_MP4;
            editImage.setVisibility(View.GONE);
        }

        return true;
    }

    public MessageMultiSelectDelegate getMultiSelect() {
        return multiSelect;
    }

    public DataSource getDataSource() {
        return source;
    }
}
