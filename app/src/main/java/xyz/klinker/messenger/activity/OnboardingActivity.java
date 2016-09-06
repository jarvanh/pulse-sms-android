package xyz.klinker.messenger.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import xyz.klinker.messenger.R;

public class OnboardingActivity extends AppIntro {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        addSlide(AppIntroFragment.newInstance(
                getString(R.string.app_name_long), "",
                R.drawable.launcher_hi_res,
                getResources().getColor(R.color.materialTeal)));

        addSlide(AppIntroFragment.newInstance(
                "Messaging Re-invented", "Smooth, fluid layouts with a powerful design and seamless integration. Pulse puts your conversations front and center.",
                R.drawable.ic_inbox,
                getResources().getColor(R.color.materialBlueGrey)));

        addSlide(AppIntroFragment.newInstance(
                "Available Anywhere", "Message from your tablet, computer, or any other connected devices!",
                R.drawable.ic_devices,
                getResources().getColor(R.color.materialLightBlue)));

        // Hide Skip/Done button.
        showSkipButton(false);

        final ViewPager pager = (ViewPager) findViewById(R.id.view_pager);
        final Button done = (Button) findViewById(R.id.done);
        final Button skip = (Button) findViewById(R.id.skip);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
            @Override public void onPageScrollStateChanged(int state) { }
            @Override public void onPageSelected(int position) {
                if (position == pager.getAdapter().getCount() - 1) {
                    skip.setVisibility(View.VISIBLE);
                } else {
                    skip.setVisibility(View.GONE);
                }
            }
        });

        done.setText("Start\nTrial");
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // start trial
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // finish without starting trial
    }
}
