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

package xyz.klinker.messenger.adapter.view_holder;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import xyz.klinker.android.article.ArticleIntent;
import xyz.klinker.messenger.BuildConfig;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.ImageViewerActivity;
import xyz.klinker.messenger.data.ArticlePreview;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.FeatureFlags;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.fragment.MessageListFragment;
import xyz.klinker.messenger.receiver.MessageListUpdatedReceiver;
import xyz.klinker.messenger.util.ColorUtils;
import xyz.klinker.messenger.util.DensityUtil;
import xyz.klinker.messenger.util.FileUtils;
import xyz.klinker.messenger.util.ImageUtils;
import xyz.klinker.messenger.util.MediaSaver;
import xyz.klinker.messenger.util.listener.ForcedRippleTouchListener;
import xyz.klinker.messenger.util.listener.MessageDeletedListener;
import xyz.klinker.messenger.util.media.parsers.ArticleParser;
import xyz.klinker.messenger.util.media.parsers.YoutubeParser;
import xyz.klinker.messenger.util.multi_select.MessageMultiSelectDelegate;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * View holder for working with a message.
 */
public class MessageViewHolder extends SwappingHolder {

    private MessageListFragment fragment;
    private MessageDeletedListener messageDeletedListener;

    public TextView message;
    public TextView timestamp;
    public TextView contact;
    public TextView title;
    public ImageView image;
    public ImageView clippedImage;
    public View messageHolder;
    public long messageId;
    public String data;
    public String mimeType;
    public int color = -1;
    public int textColor = -1;
    private int type;
    private int timestampHeight;

    private int primaryColor = -1;
    private int accentColor = -1;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (type == Message.TYPE_INFO || fragment.getMultiSelect() == null ||
                    fragment.getMultiSelect().tapSelection(MessageViewHolder.this)) {
                return;
            } else if (mimeType.equals(MimeType.MEDIA_ARTICLE)) {
                startArticle();
                return;
            }

            ValueAnimator animator;
            if (timestamp.getHeight() > 0) {
                animator = ValueAnimator.ofInt(timestampHeight, 0);
                animator.setInterpolator(new AccelerateInterpolator());
            } else {
                animator = ValueAnimator.ofInt(0, timestampHeight);
                animator.setInterpolator(new DecelerateInterpolator());
            }

            final ViewGroup.LayoutParams params = timestamp.getLayoutParams();
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    params.height = (Integer) animation.getAnimatedValue();
                    timestamp.requestLayout();
                }
            });
            animator.setDuration(100);
            animator.start();
        }
    };

    private View.OnLongClickListener messageOptions = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(final View view) {
            if (fragment.isRecyclerScrolling()) {
                return true;
            }

            if (message.getVisibility() == View.VISIBLE && type != Message.TYPE_ERROR &&
                    fragment.getMultiSelect() != null && type != Message.TYPE_INFO) {

                if (!fragment.getMultiSelect().isSelectable()) {
                    // start the multi-select
                    fragment.getMultiSelect().startActionMode();
                    fragment.getMultiSelect().setSelectable(true);
                    fragment.getMultiSelect().setSelected(MessageViewHolder.this, true);
                }

                return true;
            }

            String[] items;
            if (message.getVisibility() == View.VISIBLE) {
                if (type == Message.TYPE_ERROR) {
                    items = new String[5];
                    items[4] = view.getContext().getString(R.string.resend);
                } else {
                    items = new String[4];
                }

                items[0] = view.getContext().getString(R.string.view_details);
                items[1] = view.getContext().getString(R.string.delete);
                items[2] = view.getContext().getString(R.string.copy_message);
                items[3] = view.getContext().getString(R.string.share);
            } else {
                if (image.getVisibility() == View.VISIBLE) {
                    items = new String[4];
                    items[3] = view.getContext().getString(R.string.save);
                    items[2] = view.getContext().getString(R.string.share);
                } else {
                    items = new String[2];
                }

                items[0] = view.getContext().getString(R.string.view_details);
                items[1] = view.getContext().getString(R.string.delete);
            }

            if (fragment == null || !fragment.isDragging()) {
                AlertDialog dialog = new AlertDialog.Builder(view.getContext())
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                if (which == 0) {
                                    showMessageDetails();
                                } else if (which == 1) {
                                    deleteMessage();
                                } else if (which == 2 && image.getVisibility() == View.VISIBLE) {
                                    shareImage(messageId);
                                }else if (which == 2) {
                                    copyMessageText();
                                } else if (which == 3 && image.getVisibility() == View.VISIBLE) {
                                    new MediaSaver(fragment.getActivity()).saveMedia(messageId);
                                } else if (which == 3) {
                                    shareText(messageId);
                                } else if (which == 4) {
                                    resendMessage();
                                }
                            }
                        })
                        .show();

                if (fragment != null) {
                    fragment.setDetailsChoiceDialog(dialog);
                }
            }

            // need to manually force the haptic feedback, since the feedback was actually
            // disabled on the long clicked views
            view.setHapticFeedbackEnabled(true);
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            view.setHapticFeedbackEnabled(false);

            return false;
        }
    };

    public MessageViewHolder(final MessageListFragment fragment, final View itemView,
                             int color, final long conversationId, int type, int timestampHeight,
                             MessageDeletedListener messageDeletedListener) {
        super(itemView, fragment == null || fragment.getMultiSelect() == null ? new MultiSelector() : fragment.getMultiSelect());

        this.fragment = fragment;
        this.type = type;
        this.timestampHeight = timestampHeight;
        this.messageDeletedListener = messageDeletedListener;

        message = (TextView) itemView.findViewById(R.id.message);
        timestamp = (TextView) itemView.findViewById(R.id.timestamp);
        contact = (TextView) itemView.findViewById(R.id.contact);
        title = (TextView) itemView.findViewById(R.id.title);
        image = (ImageView) itemView.findViewById(R.id.image);
        clippedImage = (ImageView) itemView.findViewById(R.id.clipped_image);
        messageHolder = itemView.findViewById(R.id.message_holder);

        Settings settings = Settings.get(itemView.getContext());
        message.setTextSize(settings.largeFont);
        timestamp.setTextSize(settings.smallFont);
        timestamp.setHeight(DensityUtil.spToPx(itemView.getContext(), settings.mediumFont));

        if ((color != -1 && messageHolder != null) ||
                settings.useGlobalThemeColor && type == Message.TYPE_RECEIVED) {
            if (settings.useGlobalThemeColor) {
                color = Settings.get(itemView.getContext()).globalColorSet.color;
            }

            messageHolder.setBackgroundTintList(ColorStateList.valueOf(color));
            this.color = color;

            if (!ColorUtils.isColorDark(color)) {
                textColor = itemView.getContext().getResources().getColor(R.color.darkText);
            } else {
                textColor = itemView.getContext().getResources().getColor(R.color.lightText);
            }

            message.setTextColor(textColor);
        }

        if (image != null) {
            image.setOnClickListener(view -> {
                if (fragment.getMultiSelect() != null && fragment.getMultiSelect().isSelectable()) {
                    messageHolder.performClick();
                    return;
                }

                if (mimeType != null && MimeType.isVcard(mimeType)) {
                    Uri uri = Uri.parse(message.getText().toString());
                    if (message.getText().toString().contains("file://")) {
                        uri = ImageUtils.createContentUri(itemView.getContext(), uri);
                    }

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setDataAndType(uri, MimeType.TEXT_VCARD);
                    itemView.getContext().startActivity(intent);
                } else if (mimeType.equals(MimeType.MEDIA_YOUTUBE)) {
                    itemView.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                            YoutubeParser.getVideoUriFromThumbnail(data)
                    )));
                } else if (mimeType.equals(MimeType.MEDIA_ARTICLE)) {
                    startArticle();
                } else {
                    Intent intent = new Intent(itemView.getContext(), ImageViewerActivity.class);
                    intent.putExtra(ImageViewerActivity.EXTRA_CONVERSATION_ID, conversationId);
                    intent.putExtra(ImageViewerActivity.EXTRA_MESSAGE_ID, messageId);
                    itemView.getContext().startActivity(intent);
                }
            });

            if (fragment != null) {
                image.setOnLongClickListener(messageOptions);
                image.setHapticFeedbackEnabled(false);
            }
        }

        if (message != null) {
            if (fragment != null) {
                message.setOnLongClickListener(messageOptions);
                message.setOnClickListener(clickListener);
            }

            if (messageHolder != null) {
                messageHolder.setOnClickListener(clickListener);
            }

            message.setOnTouchListener(new ForcedRippleTouchListener(message));
            message.setHapticFeedbackEnabled(false);
        }
    }

    private void showMessageDetails() {
        DataSource source = DataSource.getInstance(message.getContext());
        source.open();

        new AlertDialog.Builder(message.getContext())
                .setMessage(source.getMessageDetails(message.getContext(), messageId))
                .setPositiveButton(android.R.string.ok, null)
                .show();

        source.close();
    }

    private void deleteMessage() {
        new AlertDialog.Builder(itemView.getContext())
                .setTitle(R.string.delete_message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        DataSource source = DataSource.getInstance(message.getContext());
                        source.open();
                        Message m = source.getMessage(messageId);

                        if (m != null) {
                            long conversationId = m.conversationId;
                            source.deleteMessage(messageId);
                            MessageListUpdatedReceiver.sendBroadcast(message.getContext(), conversationId);
                        }

                        if (messageDeletedListener != null && m != null) {
                            messageDeletedListener.onMessageDeleted(message.getContext(), m.conversationId,
                                    getAdapterPosition());
                        }

                        source.close();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    private void copyMessageText() {
        ClipboardManager clipboard = (ClipboardManager)
                message.getContext().getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("messenger",
                message.getText().toString());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(message.getContext(), R.string.message_copied_to_clipboard,
                Toast.LENGTH_SHORT).show();
    }

    private void resendMessage() {
        fragment.resendMessage(messageId, message.getText().toString());
    }

    private void shareImage(long messageId) {
        Message message = getMessage(messageId);

        Uri contentUri =
                ImageUtils.createContentUri(itemView.getContext(), Uri.parse(message.data));

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.setType(message.mimeType);
        itemView.getContext().startActivity(Intent.createChooser(shareIntent,
                itemView.getContext().getResources().getText(R.string.share_content)));
    }

    private void shareText(long messageId) {
        Message message = getMessage(messageId);

        if (message != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, message.data);
            shareIntent.setType(message.mimeType);
            itemView.getContext().startActivity(Intent.createChooser(shareIntent,
                    itemView.getContext().getResources().getText(R.string.share_content)));
        }
    }

    private Message getMessage(long messageId) {
        DataSource source = DataSource.getInstance(itemView.getContext());
        source.open();
        Message message = source.getMessage(messageId);
        source.close();

        return message;
    }

    public void setColors(int color, int accentColor) {
        this.primaryColor = color;
        this.accentColor = accentColor;
    }

    private void startArticle() {
        ArticleIntent intent = new ArticleIntent.Builder(itemView.getContext(), ArticleParser.ARTICLE_API_KEY)
                .setToolbarColor(primaryColor)
                .setAccentColor(accentColor)
                .setTheme(Settings.get(itemView.getContext()).isCurrentlyDarkTheme() ?
                        ArticleIntent.THEME_DARK : ArticleIntent.THEME_LIGHT)
                .setTextSize(Settings.get(itemView.getContext()).mediumFont + 1)
                .build();

        ArticlePreview preview = ArticlePreview.build(data);
        if (preview != null) {
            intent.launchUrl(itemView.getContext(), Uri.parse(preview.webUrl));
        }
    }
}
