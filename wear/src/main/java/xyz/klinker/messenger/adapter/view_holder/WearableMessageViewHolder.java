package xyz.klinker.messenger.adapter.view_holder;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import xyz.klinker.android.article.ArticleIntent;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.shared.data.ArticlePreview;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.DensityUtil;
import xyz.klinker.messenger.shared.util.ImageUtils;
import xyz.klinker.messenger.shared.util.listener.ForcedRippleTouchListener;
import xyz.klinker.messenger.shared.util.media.parsers.ArticleParser;
import xyz.klinker.messenger.shared.util.media.parsers.YoutubeParser;

public class WearableMessageViewHolder extends RecyclerView.ViewHolder {

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
            ValueAnimator animator;
            if (timestamp.getHeight() > 0) {
                animator = ValueAnimator.ofInt(timestampHeight, 0);
                animator.setInterpolator(new AccelerateInterpolator());
            } else {
                animator = ValueAnimator.ofInt(0, timestampHeight);
                animator.setInterpolator(new DecelerateInterpolator());
            }

            final ViewGroup.LayoutParams params = timestamp.getLayoutParams();
            animator.addUpdateListener(animation -> {
                params.height = (Integer) animation.getAnimatedValue();
                timestamp.requestLayout();
            });
            animator.setDuration(100);
            animator.start();
        }
    };

    public WearableMessageViewHolder(final View itemView, int color, final long conversationId, int type,
                                     int timestampHeight) {
        super(itemView);

        this.type = type;
        this.timestampHeight = timestampHeight;

        if (type == -1) {
            // footer
            return;
        }

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

        if (contact != null) {
            contact.setTextSize(settings.smallFont);
            contact.setHeight(DensityUtil.spToPx(itemView.getContext(), settings.mediumFont));
        }

        if ((color != -1 && messageHolder != null) ||
                settings.useGlobalThemeColor && type == Message.Companion.getTYPE_RECEIVED()) {
            if (settings.useGlobalThemeColor) {
                color = Settings.get(itemView.getContext()).mainColorSet.color;
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
            image.setOnClickListener(v -> {
                if (mimeType != null && MimeType.INSTANCE.isVcard(mimeType)) {
                    Uri uri = Uri.parse(message.getText().toString());
                    if (message.getText().toString().contains("file://")) {
                        uri = ImageUtils.createContentUri(itemView.getContext(), uri);
                    }

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setDataAndType(uri, MimeType.INSTANCE.getTEXT_VCARD());
                    itemView.getContext().startActivity(intent);
                } else if (mimeType != null && mimeType.equals(MimeType.INSTANCE.getMEDIA_YOUTUBE_V2())) {
//                        YouTubePreview preview = YouTubePreview.build(data);
//                        if (preview != null) {
//                            itemView.getContext().startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(preview.url)));
//                        }
                } else if (mimeType != null && mimeType.equals(MimeType.INSTANCE.getMEDIA_ARTICLE())) {
                    startArticle();
                } else {
//                    Intent intent = new Intent(itemView.getContext(), ImageViewerActivity.class);
//                    intent.putExtra(ImageViewerActivity.EXTRA_CONVERSATION_ID, conversationId);
//                    intent.putExtra(ImageViewerActivity.EXTRA_MESSAGE_ID, messageId);
//                    itemView.getContext().startActivity(intent);
                }
            });
        }

        if (message != null) {
            if (messageHolder != null) {
                messageHolder.setOnClickListener(clickListener);
            }

            message.setOnClickListener(clickListener);
            message.setOnTouchListener(new ForcedRippleTouchListener(message));
            message.setHapticFeedbackEnabled(false);
        }
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

        ArticlePreview preview = ArticlePreview.Companion.build(data);
        if (preview != null) {
            intent.launchUrl(itemView.getContext(), Uri.parse(preview.getWebUrl()));
        }
    }
}
