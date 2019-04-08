package xyz.klinker.messenger.fragment.message

import android.animation.ValueAnimator
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestion
import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.util.DensityUtil

class SmartReplyManager(private val fragment: MessageListFragment) {

    private val activity: FragmentActivity? by lazy { fragment.activity }
    private val handler: Handler by lazy { Handler() }

    private val smartReplyContainer: LinearLayout by lazy { fragment.rootView!!.findViewById(R.id.smart_reply_container) as LinearLayout }
    private val messageEntry: EditText by lazy { fragment.rootView!!.findViewById<View>(R.id.message_entry) as EditText }

    fun applySuggestions(suggestions: List<SmartReplySuggestion>) {
        Log.v("SmartReplyManager", "Suggestions size: ${suggestions.size}")

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (suggestions.isEmpty()) {
                hideContainer()
                return@postDelayed
            }

            showContainer {
                suggestions.map { suggestion ->
                    val layout = LayoutInflater.from(activity!!).inflate(R.layout.item_smart_reply_suggestion, smartReplyContainer, false)
                    val tv = layout.findViewById<TextView>(R.id.suggestion)
                    tv.text = suggestion.text

                    layout.setOnClickListener {
                        messageEntry.setText(suggestion.text)
                        messageEntry.setSelection(suggestion.text.length)
                        messageEntry.requestFocus()

                        hideContainer()
                    }

                    layout
                }.forEach { view ->
                    smartReplyContainer.addView(view)
                }
            }
        }, 1000)
    }

    private fun hideContainer() {
        if (smartReplyContainer.visibility == View.GONE) {
            smartReplyContainer.removeAllViews()
            return
        }

        val params = smartReplyContainer.layoutParams
        val animator = ValueAnimator.ofInt(smartReplyContainer.measuredHeight, 0)
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            params.height = value
            smartReplyContainer.layoutParams = params

            if (value == 0) {
                smartReplyContainer.visibility = View.GONE
                smartReplyContainer.removeAllViews()
            }
        }
        animator.interpolator = ANIMATION_INTERPOLATOR
        animator.duration = ANIMATION_DURATION
        animator.startDelay = ANIMATION_DURATION
        animator.start()

        smartReplyContainer.animate()
                .alpha(0F)
                .setInterpolator(ANIMATION_INTERPOLATOR)
                .setStartDelay(ANIMATION_START_DELAY)
                .setDuration(ANIMATION_DURATION)
                .start()
    }

    private fun showContainer(loadViewsIntoContainer: () -> Unit) {
        smartReplyContainer.removeAllViews()

        if (smartReplyContainer.visibility == View.VISIBLE) {
            // animate it out, to remove the view, then back in, since we will be filling it with new views
            smartReplyContainer.animate()
                    .alpha(0f)
                    .setInterpolator(ANIMATION_INTERPOLATOR)
                    .setDuration(ANIMATION_DURATION)
                    .start()

            Handler().postDelayed({
                smartReplyContainer.removeAllViews()
                loadViewsIntoContainer()

                smartReplyContainer.animate()
                        .alpha(100f)
                        .setInterpolator(ANIMATION_INTERPOLATOR)
                        .setDuration(ANIMATION_DURATION)
                        .start()
            }, ANIMATION_DURATION)
            return
        }

        loadViewsIntoContainer()
        smartReplyContainer.layoutParams.height = 0
        smartReplyContainer.alpha = 0f
        smartReplyContainer.visibility = View.VISIBLE

        val params = smartReplyContainer.layoutParams
        val animator = ValueAnimator.ofInt(0, DensityUtil.toDp(activity, 56))
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            params.height = value
            smartReplyContainer.layoutParams = params
        }
        animator.interpolator = ANIMATION_INTERPOLATOR
        animator.duration = ANIMATION_DURATION
        animator.startDelay = ANIMATION_START_DELAY
        animator.start()

        smartReplyContainer.animate()
                .alpha(100f)
                .setInterpolator(ANIMATION_INTERPOLATOR)
                .setStartDelay(ANIMATION_DURATION)
                .setDuration(ANIMATION_DURATION)
                .start()
    }

    companion object {
        private val ANIMATION_INTERPOLATOR = AccelerateInterpolator()
        private const val ANIMATION_DURATION = 300L
        private const val ANIMATION_START_DELAY = 0L
    }
}