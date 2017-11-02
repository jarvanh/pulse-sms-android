package xyz.klinker.messenger.activity.share

import xyz.klinker.android.floating_tutorial.FloatingTutorialActivity
import xyz.klinker.android.floating_tutorial.TutorialFinishedListener
import xyz.klinker.android.floating_tutorial.TutorialPage
import xyz.klinker.messenger.R

class QuickShareActivity : FloatingTutorialActivity(), TutorialFinishedListener {
    override fun getPages(): List<TutorialPage> = listOf(QuickSharePage(this))
    override fun onTutorialFinished() {

    }
}

class QuickSharePage(activity: FloatingTutorialActivity) : TutorialPage(activity) {
    override fun initPage() {
        setContentView(R.layout.page_quick_share)
        setNextButtonText(R.string.send)
    }
}