package xyz.klinker.messenger.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import xyz.klinker.messenger.fragment.MediaGridFragment;
import xyz.klinker.messenger.fragment.settings.ContactSettingsFragment;
import xyz.klinker.messenger.util.ColorUtils;

public class MediaGridActivity extends AppCompatActivity {
    public static final String EXTRA_CONVERSATION_ID = "conversation_id";

    private MediaGridFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fragment = MediaGridFragment.newInstance(
                getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1));

        getFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment)
                .commit();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ColorUtils.updateRecentsEntry(this);
        ColorUtils.checkBlackBackground(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return true;
    }
}
