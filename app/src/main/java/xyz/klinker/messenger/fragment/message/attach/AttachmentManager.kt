package xyz.klinker.messenger.fragment.message.attach

import android.animation.ValueAnimator
import android.content.res.Configuration
import android.net.Uri
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import xyz.klinker.messenger.R
import xyz.klinker.messenger.fragment.message.MessageListFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.util.DensityUtil
import xyz.klinker.messenger.view.SelectedAttachmentView

class AttachmentManager(private val fragment: MessageListFragment) {

    private val activity: FragmentActivity? by lazy { fragment.activity }

    val argManager
        get() = fragment.argManager

    var currentlyAttached: MutableSet<SelectedAttachmentView> = mutableSetOf()
    var editingImage: SelectedAttachmentView? = null

    private val attachLayout: View? by lazy { fragment.rootView?.findViewById<View>(R.id.attach_layout) }
    private val attachedImageScroller: HorizontalScrollView by lazy { fragment.rootView!!.findViewById<HorizontalScrollView>(R.id.attached_image_scroller) }
    private val attachedImageHolder: LinearLayout by lazy { fragment.rootView!!.findViewById<LinearLayout>(R.id.attached_image_holder) }
    private val attach: View by lazy { fragment.rootView!!.findViewById<View>(R.id.attach) }
    private val messageEntry: EditText by lazy { fragment.rootView!!.findViewById<View>(R.id.message_entry) as EditText }

    fun setupHelperViews() {

    }

    fun clearAttachedData() {
        Thread {
            if (activity != null) {
                DataSource.deleteDrafts(activity!!, argManager.conversationId)
            }
        }.start()

        hideAttachments()
        currentlyAttached.clear()
        attachedImageHolder.removeAllViews()

        fragment.counterCalculator.updateCounterText()
    }

    fun writeDraftOfAttachment() {
        currentlyAttached.forEach {
            DataSource.insertDraft(activity, argManager.conversationId, it.mediaUri.toString(), it.mimeType)
        }
    }

    fun editingImage(view: SelectedAttachmentView) {
        this.editingImage = view
    }

    fun removeAttachment(uri: Uri?) {
        val view = currentlyAttached.firstOrNull { it.mediaUri == uri } ?: return
        attachedImageHolder.removeView(view.view)
        currentlyAttached.remove(view)

        if (currentlyAttached.isEmpty()) {
            hideAttachments()
        }
    }

    fun attachMedia(uri: Uri?, mimeType: String) {
        if (activity == null || uri == null) {
            return
        }

        val view = SelectedAttachmentView(activity!!)
        view.setup(this, uri, mimeType)

        currentlyAttached.add(view)
        attachedImageHolder.addView(view.view)
        showAttachments()

        fragment.counterCalculator.updateCounterText()
    }

    fun backPressed() = if (attachLayout?.visibility == View.VISIBLE) {
        attach.isSoundEffectsEnabled = false
        attach.performClick()
        attach.isSoundEffectsEnabled = true

        fragment.attachInitializer.onClose()
        true
    } else {
        false
    }

    private fun showAttachments() {
        if (attachedImageScroller.visibility == View.VISIBLE) {
            return
        }

        attachedImageScroller.layoutParams.height = 0
        attachedImageScroller.alpha = 0f
        attachedImageScroller.visibility = View.VISIBLE

        val params = attachedImageScroller.layoutParams
        val animator = ValueAnimator.ofInt(0, DensityUtil.toDp(activity, 124)) // 96 for image size + 12 for margin top on image + 16 for padding top/bottom on scroller
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            params.height = value
            attachedImageScroller.layoutParams = params
        }
        animator.interpolator = ANIMATION_INTERPOLATOR
        animator.duration = ANIMATION_DURATION
        animator.startDelay = ANIMATION_START_DELAY
        animator.start()

        attachedImageScroller.animate()
                .alpha(100f)
                .setInterpolator(ANIMATION_INTERPOLATOR)
                .setStartDelay(ANIMATION_DURATION)
                .setDuration(ANIMATION_DURATION)
                .start()

        setMaxLines(3)
    }

    private fun hideAttachments() {
        val params = attachedImageScroller.layoutParams
        val animator = ValueAnimator.ofInt(attachedImageScroller.measuredHeight, 0)
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            params.height = value
            attachedImageScroller.layoutParams = params

            if (value == 0) {
                attachedImageScroller.visibility = View.GONE
            }
        }
        animator.interpolator = ANIMATION_INTERPOLATOR
        animator.duration = ANIMATION_DURATION
        animator.startDelay = ANIMATION_DURATION
        animator.start()

        attachedImageScroller.animate()
                .alpha(0F)
                .setInterpolator(ANIMATION_INTERPOLATOR)
                .setStartDelay(ANIMATION_START_DELAY)
                .setDuration(ANIMATION_DURATION)
                .start()

        setMaxLines(activity?.resources?.getInteger(R.integer.message_list_fragment_line_entry_count) ?: 6)
    }

    private fun setMaxLines(value: Int) {
        val orientation = activity?.resources?.configuration?.orientation
        if (activity?.resources?.getBoolean(R.bool.is_tablet) == false && orientation == Configuration.ORIENTATION_PORTRAIT) {
            messageEntry.maxLines = value
        }
    }

    companion object {
        private val ANIMATION_INTERPOLATOR = DecelerateInterpolator()
        private const val ANIMATION_DURATION = 200L
        private const val ANIMATION_START_DELAY = 0L
    }
}