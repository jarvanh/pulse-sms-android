package xyz.klinker.messenger.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import xyz.klinker.messenger.R;

public class OnboardingActivity extends AppIntro {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(AppIntroFragment.newInstance(
                getString(R.string.onboarding_title_1), getString(R.string.onboarding_content_1),
                R.drawable.ic_onboarding_inbox,
                getResources().getColor(R.color.materialTeal)));

        addSlide(AppIntroFragment.newInstance(
                getString(R.string.onboarding_title_2), getString(R.string.onboarding_content_2),
                R.drawable.ic_onboarding_devices,
                getResources().getColor(R.color.materialLightBlue)));

        addSlide(AppIntroFragment.newInstance(
                getString(R.string.onboarding_title_3), getString(R.string.onboarding_content_3),
                R.drawable.ic_onboarding_person,
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
