package xyz.klinker.messenger.activity.notification

import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.ScrollView
import xyz.klinker.messenger.R

class ReplyAnimators(private val activity: MarshmallowReplyActivity) {

    private val scrollView: ScrollView by lazy { activity.findViewById<View>(R.id.scroll_view) as ScrollView }
    private val messagesInitial: LinearLayout by lazy { activity.findViewById<View>(R.id.messages_initial) as LinearLayout }
    private val messagesInitialHolder: LinearLayout by lazy { activity.findViewById<View>(R.id.messages_initial_holder) as LinearLayout }
    private val sendBar: LinearLayout by lazy { activity.findViewById<View>(R.id.send_bar) as LinearLayout }

    fun alphaOut(view: View, duration: Long, startDelay: Long) {
        view.animate().withLayer()
                .alpha(0f).setDuration(duration).setStartDelay(startDelay).setListener(null)
    }

    fun alphaIn(view: View, duration: Long, startDelay: Long) {
        view.animate().withLayer()
                .alpha(1f).setDuration(duration).setStartDelay(startDelay).setListener(null)
    }

    fun bounceIn() {
        scrollView.translationY = (messagesInitial.height + sendBar.height) * (-1).toFloat()
        scrollView.animate().withLayer()
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(OvershootInterpolator())
                .setListener(null)
    }

    fun slideOut() {
        scrollView.smoothScrollTo(0, scrollView.bottom)

        val translation = ((messagesInitialHolder.height + sendBar.height) * -1).toFloat()
        scrollView.animate().withLayer()
                .translationY(translation)
                .setDuration(100)
                .setInterpolator(AccelerateInterpolator())
                .setListener(null)
    }
}