package xyz.klinker.messenger.fragment.message.load

import android.view.View
import android.widget.EditText
import xyz.klinker.messenger.R
import xyz.klinker.messenger.fragment.MessageListFragment
import xyz.klinker.messenger.shared.util.DualSimApplication
import xyz.klinker.messenger.view.ElasticDragDismissFrameLayout
import xyz.klinker.messenger.view.ImageKeyboardEditText

class ViewInitializerDeferred(private val fragment: MessageListFragment) {

    private val activity
        get() = fragment.activity

    val dragDismissFrameLayout: ElasticDragDismissFrameLayout by lazy { fragment.rootView as ElasticDragDismissFrameLayout }
    private val messageEntry: EditText by lazy { fragment.rootView!!.findViewById<View>(R.id.message_entry) as EditText }
    private val selectSim: View by lazy { fragment.rootView!!.findViewById<View>(R.id.select_sim) }

    fun init() {
        fragment.sendManager.initSendbar()
        fragment.attachManager.setupHelperViews()
        fragment.attachInitializer.initAttachHolder()

        dragDismissFrameLayout.addListener(object : ElasticDragDismissFrameLayout.ElasticDragDismissCallback() {
            override fun onDrag(elasticOffset: Float, elasticOffsetPixels: Float, rawOffset: Float, rawOffsetPixels: Float) { }
            override fun onDragDismissed() {
                fragment.dismissKeyboard()
                activity.onBackPressed()
            }
        })

        if (messageEntry is ImageKeyboardEditText) {
            (messageEntry as ImageKeyboardEditText).setCommitContentListener(fragment.attachListener)
        }

        try {
            DualSimApplication(selectSim).apply(fragment.argManager.conversationId)
        } catch (e: Exception) {
        }
    }
}