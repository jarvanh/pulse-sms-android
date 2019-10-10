package xyz.klinker.messenger.fragment.message.attach

import android.net.Uri
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import xyz.klinker.messenger.R
import xyz.klinker.messenger.fragment.message.MessageListFragment
import xyz.klinker.messenger.shared.data.DataSource
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

    fun setupHelperViews() {

    }

    fun clearAttachedData() {
        Thread {
            if (activity != null) {
                DataSource.deleteDrafts(activity!!, argManager.conversationId)
            }
        }.start()

        currentlyAttached.clear()
        attachedImageScroller.visibility = View.GONE
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
            attachedImageScroller.visibility = View.GONE
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
        attachedImageScroller.visibility = View.VISIBLE

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
}