package xyz.klinker.messenger.fragment.bottom_sheet;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import java.util.Random;

import xyz.klinker.android.article.ArticleIntent;
import xyz.klinker.messenger.BuildConfig;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.ComposeActivity;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Message;

import static android.content.Context.CLIPBOARD_SERVICE;

public class LinkLongClickFragment extends TabletOptimizedBottomSheetDialogFragment {

    private String link;
    private int mainColor;
    private int accentColor;

    @Override
    protected View createLayout(LayoutInflater inflater) {
        final View contentView = View.inflate(getContext(), R.layout.bottom_sheet_link, null);

        View openExternal = contentView.findViewById(R.id.open_external);
        View openInternal = contentView.findViewById(R.id.open_internal);
        View copyText = contentView.findViewById(R.id.copy_text);

        openExternal.setOnClickListener(view -> {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(mainColor);
            builder.setShowTitle(true);
            builder.setActionButton(
                    BitmapFactory.decodeResource(getResources(), R.drawable.ic_share),
                    getString(R.string.share), getShareIntent(link), true);
            CustomTabsIntent customTabsIntent = builder.build();

            customTabsIntent.launchUrl(getActivity(), Uri.parse(link));
            dismiss();
        });

        openInternal.setOnClickListener(view -> {
            ArticleIntent intent = new ArticleIntent.Builder(contentView.getContext(), BuildConfig.ARTICLE_API_KEY)
                    .setToolbarColor(mainColor)
                    .setAccentColor(accentColor)
                    .setTheme(Settings.get(contentView.getContext()).isCurrentlyDarkTheme() ?
                            ArticleIntent.THEME_DARK : ArticleIntent.THEME_LIGHT)
                    .build();

            intent.launchUrl(contentView.getContext(), Uri.parse(link));
            dismiss();
        });

        copyText.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager)
                    getActivity().getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("messenger",link);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getActivity(), R.string.message_copied_to_clipboard,
                    Toast.LENGTH_SHORT).show();

            dismiss();
        });

        return contentView;
    }


    public void setLink(String link) {
        this.link = link;
    }

    public void setColors(int mainColor, int accentColor) {
        this.mainColor = mainColor;
        this.accentColor = accentColor;
    }

    private PendingIntent getShareIntent(String url) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, url);
        shareIntent.setType(MimeType.TEXT_PLAIN);
        return PendingIntent.getActivity(getActivity(), new Random().nextInt(Integer.MAX_VALUE), shareIntent, 0);
    }
}
