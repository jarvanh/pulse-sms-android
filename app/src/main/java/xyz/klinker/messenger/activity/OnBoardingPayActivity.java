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
import xyz.klinker.messenger.fragment.settings.MyAccountFragment;
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper;

public class OnBoardingPayActivity extends AppIntro {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsHelper.accountTutorialStarted(this);

        setResult(MyAccountFragment.RESPONSE_SKIP_TRIAL_FOR_NOW);
        showSkipButton(true);

        addSlide(AppIntroFragment.newInstance(
                getString(R.string.message_anywhere_onboarding_title_1), getString(R.string.message_anywhere_onboarding_content_1),
                R.drawable.ic_onboarding_computer,
                getResources().getColor(R.color.materialIndigo)));

        addSlide(AppIntroFragment.newInstance(
                getString(R.string.message_anywhere_onboarding_title_3), getString(R.string.message_anywhere_onboarding_content_3),
                R.drawable.ic_onboarding_redeem,
                getResources().getColor(R.color.materialTeal)));

        addSlide(AppIntroFragment.newInstance(
                getString(R.string.message_anywhere_onboarding_title_2), getString(R.string.message_anywhere_onboarding_content_2),
                R.drawable.ic_onboarding_subscription,
                getResources().getColor(R.color.materialBlueGrey)));

        final ViewPager pager = (ViewPager) findViewById(R.id.view_pager);
        final Button done = (Button) findViewById(R.id.done);
        final Button skip = (Button) findViewById(R.id.skip);

        if (LoginActivity.hasTelephony(this)) {
            skip.setText(getString(R.string.api_login));
            done.setText(getString(R.string.start_trial));
        } else {
            // just in case a tablet makes it in here, lets exit and let them sign in
            setResult(MyAccountFragment.RESPONSE_START_TRIAL);
            finish();
        }
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        AnalyticsHelper.accountTutorialFinished(this);
        setResult(MyAccountFragment.RESPONSE_START_TRIAL);
        finish();
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);

        AnalyticsHelper.accountTutorialFinished(this);
        setResult(MyAccountFragment.RESPONSE_START_TRIAL);
        finish();
    }
}
