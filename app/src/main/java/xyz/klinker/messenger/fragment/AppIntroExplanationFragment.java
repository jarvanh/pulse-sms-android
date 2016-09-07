package xyz.klinker.messenger.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;

import xyz.klinker.messenger.R;

public class AppIntroExplanationFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_intro_explanation, container, false);

        TextView description = (TextView) root.findViewById(R.id.description);
        description.setText(Html.fromHtml(getString(R.string.message_anywhere_trial_description)
                .replaceAll("%bb", "</b>").replaceAll("%b", "<b>")
                .replaceAll("%ii", "</i>").replaceAll("%i", "<i>")
                .replaceAll("\n", "<br>")));

        TextView tablet = (TextView) root.findViewById(R.id.platform_tablet);
        LinkBuilder.on(tablet)
                .addLink(new Link(tablet.getText().toString()).setPrependedText("- ")
                            .setUnderlined(false)
                            .setTextColor(getResources().getColor(R.color.materialOrangeAccent))
                            .setOnClickListener(new Link.OnClickListener() {
                                @Override
                                public void onClick(String clickedText) {
                                    openLink("https://play.google.com/store/apps/details?id=xyz.klinker.messenger");
                                }
                            }))
                .build();

        TextView web = (TextView) root.findViewById(R.id.platform_web);
        LinkBuilder.on(web)
                .addLink(new Link(web.getText().toString()).setPrependedText("- ")
                            .setUnderlined(false)
                            .setTextColor(getResources().getColor(R.color.materialOrangeAccent))
                            .setOnClickListener(new Link.OnClickListener() {
                                @Override
                                public void onClick(String clickedText) {
                                    openLink("https://messenger.klinkerapps.com");
                                }
                            }))
                .build();

        TextView chromeApp = (TextView) root.findViewById(R.id.platform_chrome_app);
        LinkBuilder.on(chromeApp)
                .addLink(new Link(chromeApp.getText().toString()).setPrependedText("- ")
                            .setUnderlined(false)
                            .setTextColor(getResources().getColor(R.color.materialOrangeAccent))
                            .setOnClickListener(new Link.OnClickListener() {
                                @Override
                                public void onClick(String clickedText) {
                                    openLink("https://chrome.google.com/webstore/detail/messenger-app/nimjciaekjijpinhkhpmcbaoanjjojfi");
                                }
                            }))
                .build();

        TextView chromeExtension = (TextView) root.findViewById(R.id.platform_chrome_extension);
        LinkBuilder.on(chromeExtension)
                .addLink(new Link(chromeExtension.getText().toString()).setPrependedText("- ")
                            .setUnderlined(false)
                            .setTextColor(getResources().getColor(R.color.materialOrangeAccent))
                            .setOnClickListener(new Link.OnClickListener() {
                                @Override
                                public void onClick(String clickedText) {
                                    openLink("https://chrome.google.com/webstore/detail/messenger-extension/jjbjdleccaiklfpcblgekgmjadjdclkp");
                                }
                            }))
                .build();

        TextView wrapUp = (TextView) root.findViewById(R.id.wrap_up);
        wrapUp.setText(Html.fromHtml(getString(R.string.trial_description_end)
                .replaceAll("%bb", "</b>").replaceAll("%b", "<b>")
                .replaceAll("%ii", "</i>").replaceAll("%i", "<i>")
                .replaceAll("\n", "<br>")));

        return root;
    }

    private void openLink(String webAddress) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(webAddress));
        startActivity(intent);
    }
}
