package xyz.klinker.messenger.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.fragment.AppIntroExplanationFragment;

public class OnboardingActivity extends AppIntro {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        addSlide(AppIntroFragment.newInstance(
                "Messaging Re-imagined", "Smooth, fluid layouts with a powerful design and seamless integration. Messenger puts your conversations front and center.",
                R.drawable.ic_inbox_onboarding,
                getResources().getColor(R.color.materialTeal)));

        addSlide(AppIntroFragment.newInstance(
                "Available Everywhere", "Message from your tablet, computer, or any other connected devices!",
                R.drawable.ic_devices_onboarding,
                getResources().getColor(R.color.materialLightBlue)));

        addSlide(new AppIntroExplanationFragment());

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

        skip.setText(getString(R.string.skip_trial));
        done.setText(getString(R.string.start_trial));
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        setResult(MessengerActivity.RESULT_START_TRIAL);
        finish();
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);

        setResult(MessengerActivity.RESULT_SKIP_TRIAL);
        finish();
    }
}
