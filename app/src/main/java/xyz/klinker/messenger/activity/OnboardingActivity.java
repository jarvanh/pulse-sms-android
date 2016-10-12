package xyz.klinker.messenger.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.implementation.LoginActivity;
import xyz.klinker.messenger.fragment.AppIntroExplanationFragment;

public class OnboardingActivity extends AppIntro {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        addSlide(AppIntroFragment.newInstance(
                getString(R.string.onboarding_title_1), getString(R.string.onboarding_content_1),
                R.drawable.ic_inbox_onboarding,
                getResources().getColor(R.color.materialTeal)));

        addSlide(AppIntroFragment.newInstance(
                getString(R.string.onboarding_title_2), getString(R.string.onboarding_content_2),
                R.drawable.ic_devices_onboarding,
                getResources().getColor(R.color.materialLightBlue)));

        addSlide(AppIntroFragment.newInstance(
                getString(R.string.onboarding_title_3), getString(R.string.onboarding_content_3),
                R.drawable.ic_person_onboarding,
                getResources().getColor(R.color.materialLightGreen)));

        showSkipButton(true);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        finish();
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        finish();
    }
}
