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
import android.view.View;
import android.widget.Toast;

import java.util.Random;

import xyz.klinker.android.article.ArticleIntent;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.ComposeActivity;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Message;

import static android.content.Context.CLIPBOARD_SERVICE;

public class LinkLongClickFragment extends BottomSheetDialogFragment {

    private String link;
    private int mainColor;
    private int accentColor;

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) { }
        @Override public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }
    };

    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        final View contentView = View.inflate(getContext(), R.layout.bottom_sheet_link, null);
        dialog.setContentView(contentView);

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();

        if (behavior != null && behavior instanceof BottomSheetBehavior ) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }

        View openExternal = contentView.findViewById(R.id.open_external);
        View openInternal = contentView.findViewById(R.id.open_internal);
        View copyText = contentView.findViewById(R.id.copy_text);

        openExternal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(mainColor);
                builder.setShowTitle(true);
                builder.setActionButton(
                        BitmapFactory.decodeResource(getResources(), R.drawable.ic_share),
                        getString(R.string.share), getShareIntent(link), true);
                CustomTabsIntent customTabsIntent = builder.build();

                customTabsIntent.launchUrl(getActivity(), Uri.parse(link));
                dialog.dismiss();
            }
        });

        openInternal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArticleIntent intent = new ArticleIntent.Builder(contentView.getContext())
                        .setToolbarColor(mainColor)
                        .setAccentColor(accentColor)
                        .setTheme(Settings.get(contentView.getContext()).isCurrentlyDarkTheme() ?
                                ArticleIntent.THEME_DARK : ArticleIntent.THEME_LIGHT)
                        .build();

                intent.launchUrl(contentView.getContext(), Uri.parse(link));
                dialog.dismiss();
            }
        });

        copyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager)
                        getActivity().getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("messenger",link);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getActivity(), R.string.message_copied_to_clipboard,
                        Toast.LENGTH_SHORT).show();

                dialog.dismiss();
            }
        });
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
