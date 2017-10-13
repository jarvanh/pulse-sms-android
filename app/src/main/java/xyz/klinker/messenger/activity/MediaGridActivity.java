package xyz.klinker.messenger.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.fragment.MediaGridFragment;
import xyz.klinker.messenger.fragment.settings.ContactSettingsFragment;
import xyz.klinker.messenger.shared.activity.AbstractSettingsActivity;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.util.ColorUtils;

public class MediaGridActivity extends AbstractSettingsActivity {
    public static final String EXTRA_CONVERSATION_ID = "conversation_id";

    private MediaGridFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fragment = MediaGridFragment.newInstance(
                getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1));

        try {
            getFragmentManager().beginTransaction()
                    .replace(R.id.settings_content, fragment)
                    .commit();
        } catch (Exception e) {

        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ColorUtils.INSTANCE.checkBlackBackground(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        ActivityUtils.INSTANCE.setTaskDescription(this, fragment.conversation.getTitle(), fragment.conversation.getColors().getColor());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return true;
    }
}
