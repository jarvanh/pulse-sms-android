package xyz.klinker.messenger.shared.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import xyz.klinker.android.floating_tutorial.FloatingTutorialActivity
import xyz.klinker.android.floating_tutorial.TutorialFinishedListener
import xyz.klinker.android.floating_tutorial.TutorialPage
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.DensityUtil

class RateItDialog : FloatingTutorialActivity(), TutorialFinishedListener {

    override fun onTutorialFinished() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Couldn't launch the Play Store!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getPages(): List<TutorialPage> {
        return listOf(object : TutorialPage(this@RateItDialog) {
            override fun initPage() {
                setContentView(R.layout.tutorial_page_rate_it)
                setNextButtonText(R.string.rate_it)

                findViewById<View>(R.id.top_text).setBackgroundColor(Settings.get(getActivity()).mainColorSet.color)
            }

            override fun animateLayout() {
                val startTime: Long = 300

                quickViewReveal(findViewById<View>(R.id.bottom_text_1), startTime)
                quickViewReveal(findViewById<View>(R.id.bottom_text_2), startTime + 75)

                quickViewReveal(findViewById<View>(R.id.star_1), startTime)
                quickViewReveal(findViewById<View>(R.id.star_2), startTime + 75)
                quickViewReveal(findViewById<View>(R.id.star_3), startTime + 150)
                quickViewReveal(findViewById<View>(R.id.star_4), startTime + 225)
                quickViewReveal(findViewById<View>(R.id.star_5), startTime + 300)
            }
        })
    }

    private fun quickViewReveal(view: View, delay: Long) {
        view.translationX = (-1 * DensityUtil.toDp(this, 16)).toFloat()
        view.alpha = 0f
        view.visibility = View.VISIBLE

        view.animate()
                .translationX(0f)
                .alpha(1f)
                .setStartDelay(delay)
                .start()
    }
}