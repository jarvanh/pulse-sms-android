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
                "Messaging Re-imagined", "Smooth, fluid layouts with a powerful design and seamless integration. Messenger puts your conversations front and center.",
                R.drawable.ic_inbox_onboarding,
                getResources().getColor(R.color.materialTeal)));

        addSlide(AppIntroFragment.newInstance(
                "Available Everywhere", "Message from your tablet, computer, watch, or any other connected devices.",
                R.drawable.ic_devices_onboarding,
                getResources().getColor(R.color.materialLightBlue)));

        addSlide(AppIntroFragment.newInstance(
                "Powerful Customization", "Pin, mute, theme, and customize notifications for any contacts you are chatting with. Make Pulse yours.",
                R.drawable.ic_person_onboarding,
                getResources().getColor(R.color.materialLightGreen)));

        showSkipButton(true);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        //setResult(MessengerActivity.RESULT_START_TRIAL);
        setResult(MessengerActivity.RESULT_SKIP_TRIAL);
        finish();
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);

        setResult(MessengerActivity.RESULT_SKIP_TRIAL);
        finish();
    }
}
