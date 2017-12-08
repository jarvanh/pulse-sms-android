package xyz.klinker.messenger.activity

import android.os.Bundle
import android.view.MenuItem
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.main.MainColorController
import xyz.klinker.messenger.fragment.MediaGridFragment
import xyz.klinker.messenger.shared.activity.AbstractSettingsActivity
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.util.ColorUtils

class MediaGridActivity : AbstractSettingsActivity() {

    private val fragment: MediaGridFragment by lazy { MediaGridFragment.newInstance(
            intent.getLongExtra(EXTRA_CONVERSATION_ID, -1)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentManager.beginTransaction()
                .replace(R.id.settings_content, fragment)
                .commit()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ColorUtils.checkBlackBackground(this)
        MainColorController(this).configureNavigationBarColor()
    }

    public override fun onStart() {
        super.onStart()
        ActivityUtils.setTaskDescription(this, fragment.conversation!!.title!!, fragment.conversation!!.colors.color)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }

        return true
    }

    companion object {
        val EXTRA_CONVERSATION_ID = "conversation_id"
    }
}
