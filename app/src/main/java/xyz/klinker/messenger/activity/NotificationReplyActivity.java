package xyz.klinker.messenger.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.graphics.Color;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.MessengerActivityExtras;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.data.pojo.BaseTheme;
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver;
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver;
import xyz.klinker.messenger.shared.service.ReplyService;
import xyz.klinker.messenger.shared.service.jobs.MarkAsSentJob;
import xyz.klinker.messenger.shared.util.ContactImageCreator;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.CursorUtil;
import xyz.klinker.messenger.shared.util.DensityUtil;
import xyz.klinker.messenger.shared.util.DualSimUtils;
import xyz.klinker.messenger.shared.util.KeyboardLayoutHelper;
import xyz.klinker.messenger.shared.util.SendUtils;
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider;

public class NotificationReplyActivity extends AppCompatActivity {

    public static final int PREV_MESSAGES_TOTAL = 10;
    public static final int PREV_MESSAGES_DISPLAYED = 3;

    private View content;
    private View dimBackground;
    private View scrollviewFiller;
    private CircleImageView image;
    private ProgressBar progressBar;
    private ScrollView scrollView;
    private LinearLayout sendBar;
    private LinearLayout messagesInitialHolder;
    private LinearLayout messagesInitial;
    private LinearLayout messagesMore;
    private EditText messageInput;
    private ImageButton sendButton;
    private TextView conversationIndicator;

    private long conversationId;
    private Conversation conversation;
    private List<Message> messages;

    @Override
    public void onBackPressed() {
        String text = messageInput.getText().toString();
        if (!text.isEmpty() && sendButton.isEnabled()) {
            DataSource source = DataSource.INSTANCE;
            source.insertDraft(this, conversationId, text, MimeType.INSTANCE.getTEXT_PLAIN());
        }

        hideKeyboard();
        slideOut();
        content.postDelayed(() -> {
            finish();
            overridePendingTransition(0, android.R.anim.fade_out);
        }, 300);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getIntent().putExtra(ReplyService.EXTRA_CONVERSATION_ID, intent.getLongExtra(ReplyService.EXTRA_CONVERSATION_ID, -1));
        recreate();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_reply);

        overridePendingTransition(0, 0);

        if (handleWearableReply()) {
            return;
        }

        conversationId = getIntent().getLongExtra(ReplyService.EXTRA_CONVERSATION_ID, -1);
        if (conversationId == -1) {
            finish();
            return;
        }

        content = findViewById(android.R.id.content);
        dimBackground = findViewById(R.id.dim_background);
        scrollviewFiller = findViewById(R.id.scrollview_filler);
        image = (CircleImageView) findViewById(R.id.image);
        progressBar = (ProgressBar) findViewById(R.id.send_progress);
        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        sendBar = (LinearLayout) findViewById(R.id.send_bar);
        messagesInitialHolder = (LinearLayout) findViewById(R.id.messages_initial_holder);
        messagesInitial = (LinearLayout) findViewById(R.id.messages_initial);
        messagesMore = (LinearLayout) findViewById(R.id.messages_more);
        messageInput = (EditText) findViewById(R.id.message_entry);
        sendButton = (ImageButton) findViewById(R.id.send_button);
        conversationIndicator = (TextView) findViewById(R.id.conversation_indicator);
        
        if (Settings.INSTANCE.getBaseTheme() == BaseTheme.BLACK) {
            messagesInitialHolder.setBackgroundColor(Color.BLACK);
            messagesMore.setBackgroundColor(Color.BLACK);
        }

        setupMessageHistory();

        if (conversation == null) {
            finish();
            return;
        }

        setupSendBar();
        setupBackgroundComponents();
        showContactImage();
        
        alphaIn(dimBackground, 300, 0);

        content.post(() -> {
            displayMessages();

            scrollviewFiller.post(() -> {
                resizeDismissibleView();
                scrollView.post(() -> showScrollView());
            });
        });

        messageInput.postDelayed(() -> {
            messageInput.requestFocus();
            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showSoftInput(messageInput, InputMethodManager.SHOW_FORCED);

            NotificationManagerCompat.from(NotificationReplyActivity.this).cancel((int) conversation.getId());
            ApiUtils.INSTANCE.dismissNotification(Account.INSTANCE.getAccountId(),
                    Account.INSTANCE.getDeviceId(), conversation.getId());
        }, 300);
    }

    // region setup message history
    private void setupMessageHistory() {
        DataSource source = DataSource.INSTANCE;

        conversation = source.getConversation(this, conversationId);
        source.seenConversation(this, conversationId);

        Cursor cursor = source.getMessages(this, conversationId);

        messages = new ArrayList<>();
        if (cursor.moveToLast()) {
            do {
                Message message = new Message();
                message.fillFromCursor(cursor);

                if (!MimeType.INSTANCE.isExpandedMedia(message.getMimeType())) {
                    messages.add(message);
                }
            } while (cursor.moveToPrevious() && messages.size() < PREV_MESSAGES_TOTAL);
        }

        CursorUtil.closeSilent(cursor);
    }

    private void displayMessages() {
        for (int i = 0; i < messages.size(); i++) {
            if (i < PREV_MESSAGES_DISPLAYED) {
                messagesInitial.addView(generateMessageTextView(messages.get(i)), 0);
            } else {
                messagesMore.addView(generateMessageTextView(messages.get(i)), 0);
            }
        }
    }

    private TextView generateMessageTextView(Message message) {
        TextView tv = new TextView(this);
        tv.setMaxLines(3);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setTextColor(getResources().getColor(R.color.primaryText));

        String string;
        if (message.getType() == Message.Companion.getTYPE_RECEIVED()) {
            string = "<b>" + (message.getFrom() != null ? message.getFrom() : conversation.getTitle()) + ":</b> ";
        } else {
            string = getString(R.string.you) + ": ";
        }

        if (MimeType.INSTANCE.isAudio(message.getMimeType())) {
            string += "<i>" + getString(R.string.audio_message) + "</i>";
        } else if (MimeType.INSTANCE.isVideo(message.getMimeType())) {
            string += "<i>" + getString(R.string.video_message) + "</i>";
        } else if (MimeType.INSTANCE.isVcard(message.getMimeType())) {
            string += "<i>" + getString(R.string.contact_card) + "</i>";
        } else if (MimeType.INSTANCE.isStaticImage(message.getMimeType())) {
            string += "<i>" + getString(R.string.picture_message) + "</i>";
        } else if (message.getMimeType().equals(MimeType.INSTANCE.getIMAGE_GIF())) {
            string += "<i>" + getString(R.string.gif_message) + "</i>";
        } else if (MimeType.INSTANCE.isExpandedMedia(message.getMimeType())) {
            string += "<i>" + getString(R.string.media) + "</i>";
        } else {
            string += message.getData();
        }

        tv.setText(Html.fromHtml(string));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = DensityUtil.INSTANCE.toDp(this, 4);
        params.bottomMargin = params.topMargin;
        tv.setLayoutParams(params);

        return tv;
    }

    private void showContactImage() {
        if (conversation.getImageUri() == null) {
            if (ContactUtils.shouldDisplayContactLetter(conversation)) {
                image.setImageBitmap(ContactImageCreator.INSTANCE.getLetterPicture(this, conversation));
            } else if (Settings.INSTANCE.getUseGlobalThemeColor()) {
                image.setImageDrawable(new ColorDrawable(Settings.INSTANCE.getMainColorSet().getColor()));
            } else {
                image.setImageDrawable(new ColorDrawable(conversation.getColors().getColor()));
            }
        } else {
            Glide.with(this)
                    .load(Uri.parse(conversation.getImageUri()))
                    .into(image);
        }
    }
    // endregion

    // region setup sendbar
    private void setupSendBar() {
        if (Settings.INSTANCE.getUseGlobalThemeColor()) {
            sendBar.setBackgroundColor(Settings.INSTANCE.getMainColorSet().getColor());
            conversationIndicator.setTextColor(Settings.INSTANCE.getMainColorSet().getColor());
            conversationIndicator.getCompoundDrawablesRelative()[2] // drawable end
                    .setTintList(ColorStateList.valueOf(Settings.INSTANCE.getMainColorSet().getColor()));
        } else {
            sendBar.setBackgroundColor(conversation.getColors().getColor());
            conversationIndicator.setTextColor(conversation.getColors().getColor());
            conversationIndicator.getCompoundDrawablesRelative()[2] // drawable end
                    .setTintList(ColorStateList.valueOf(conversation.getColors().getColor()));
        }

        conversationIndicator.setText(getString(R.string.conversation_with, conversation.getTitle()));
        conversationIndicator.setOnClickListener(v -> scrollView.smoothScrollTo(0, 0));

        sendButton.setEnabled(false);
        sendButton.setAlpha(.5f);
        sendButton.setOnClickListener(view -> {
            sendButton.setEnabled(false);

            hideKeyboard();
            sendMessage();

            alphaOut(sendButton, 200, 0);
            alphaIn(progressBar, 200, 100);

            sendButton.postDelayed(() -> onBackPressed(), 1000);
        });


        KeyboardLayoutHelper.INSTANCE.applyLayout(messageInput);
        messageInput.setHint(getString(R.string.type_message));
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override public void afterTextChanged(Editable editable) {
                if (messageInput.getText().length() > 0) {
                    sendButton.setEnabled(true);
                    sendButton.setAlpha(1f);
                } else {
                    sendButton.setEnabled(false);
                    sendButton.setAlpha(.5f);
                }
            }
        });

        messageInput.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            boolean handled = false;

            if ((keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                    keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) ||
                    actionId == EditorInfo.IME_ACTION_SEND) {
                sendButton.performClick();
                handled = true;
            }

            return handled;
        });

    }
    //endregion
    // region setup background components
    private void resizeDismissibleView() {
        ViewGroup.LayoutParams dismissableParams = scrollviewFiller.getLayoutParams();
        dismissableParams.height = content.getHeight() - sendBar.getHeight() -
                messagesInitialHolder.getHeight();
        scrollviewFiller.setLayoutParams(dismissableParams);
    }

    private void showScrollView() {
        scrollView.scrollTo(0, scrollView.getBottom());
        scrollView.setVisibility(View.VISIBLE);

        bounceIn();
    }

    private void setupBackgroundComponents() {
        dimBackground.setOnClickListener(view -> onBackPressed());
        scrollviewFiller.setOnClickListener(view -> onBackPressed());

        messagesInitialHolder.setOnClickListener(v -> {
            onBackPressed();

            Intent intent = new Intent(NotificationReplyActivity.this, MessengerActivity.class);
            intent.putExtra(MessengerActivityExtras.INSTANCE.getEXTRA_CONVERSATION_ID(), conversationId);
            intent.putExtra(MessengerActivityExtras.INSTANCE.getEXTRA_FROM_NOTIFICATION(), true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        messagesMore.setOnClickListener(v -> messagesInitialHolder.performClick());

        if (getResources().getBoolean(R.bool.is_tablet)) {
            scrollView.getLayoutParams().width = DensityUtil.INSTANCE.toDp(this, 418);
        }

        final GestureDetectorCompat detectorCompat = new GestureDetectorCompat(this,
                new GestureListener());
        scrollView.setOnTouchListener((view, motionEvent) -> {
            detectorCompat.onTouchEvent(motionEvent);
            return false;
        });
    }
    // endregion

    private void sendMessage() {

        final String message = messageInput.getText().toString().trim();

        final Message m = new Message();
        m.setConversationId(conversationId);
        m.setType(Message.Companion.getTYPE_SENDING());
        m.setData(message);
        m.setTimestamp(System.currentTimeMillis());
        m.setMimeType(MimeType.INSTANCE.getTEXT_PLAIN());
        m.setRead(true);
        m.setSeen(true);
        m.setFrom(null);
        m.setColor(null);
        m.setSimPhoneNumber(conversation.getSimSubscriptionId() != null ? DualSimUtils.Companion.get(this)
                .getPhoneNumberFromSimSubscription(conversation.getSimSubscriptionId()) : null);
        m.setSentDeviceId(Account.INSTANCE.exists() ? Long.parseLong(Account.INSTANCE.getDeviceId()) : -1L);

        // we don't have to check zero length, since the button is disabled if zero length
        DataSource source = DataSource.INSTANCE;
        final long messageId = source.insertMessage(this, m, m.getConversationId(), true);
        source.readConversation(this, conversationId);

        new Thread(() -> {
            new SendUtils(conversation.getSimSubscriptionId())
                    .send(NotificationReplyActivity.this, message, conversation.getPhoneNumbers());
            MarkAsSentJob.Companion.scheduleNextRun(this, messageId);
        }).start();

        ConversationListUpdatedReceiver.sendBroadcast(this, conversationId, getString(R.string.you) + ": " + message, true);
        MessageListUpdatedReceiver.sendBroadcast(this, conversationId);
        MessengerAppWidgetProvider.refreshWidget(this);
    }

    private boolean handleWearableReply() {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(getIntent());
        if (remoteInput != null) {
            Intent replyService = new Intent(this, ReplyService.class);
            replyService.putExtras(getIntent());

            startService(replyService);
            finish();

            return true;
        }

        return false;
    }

    private void hideKeyboard() {
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(messageInput.getWindowToken(), 0);
    }

    // region animators
    private void alphaOut(View view, long duration, long startDelay) {
        view.animate().withLayer()
                .alpha(0f).setDuration(duration).setStartDelay(startDelay).setListener(null);
    }

    private void alphaIn(View view, long duration, long startDelay) {
        view.animate().withLayer()
                .alpha(1f).setDuration(duration).setStartDelay(startDelay).setListener(null);
    }

    private void bounceIn() {
        scrollView.setTranslationY((messagesInitial.getHeight() + sendBar.getHeight()) * -1);
        scrollView.animate().withLayer()
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator())
                .setListener(null);
    }

    private void slideOut() {
        scrollView.smoothScrollTo(0, scrollView.getBottom());

        float translation = (messagesInitialHolder.getHeight() + sendBar.getHeight()) * -1;
        scrollView.animate().withLayer()
                .translationY(translation)
                .setDuration(100)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(null);
    }
    // endregion

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (velocityY < -3000 && (velocityX < 7000 && velocityX > -7000)) {
                onBackPressed();
            }

            return false;
        }
    }
}
