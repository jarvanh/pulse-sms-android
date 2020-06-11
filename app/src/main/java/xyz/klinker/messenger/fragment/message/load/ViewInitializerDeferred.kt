package xyz.klinker.messenger.fragment.message.load

import android.content.Context
import android.os.Build
import androidx.appcompat.widget.Toolbar
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.FragmentActivity
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.BubbleActivity
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.fragment.message.MessageListFragment
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.util.DualSimApplication
import xyz.klinker.messenger.shared.util.TvUtils
import xyz.klinker.messenger.view.ElasticDragDismissFrameLayout
import xyz.klinker.messenger.view.ImageKeyboardEditText

class ViewInitializerDeferred(private val fragment: MessageListFragment) {

    private val activity: FragmentActivity? by lazy { fragment.activity }

    val dragDismissFrameLayout: ElasticDragDismissFrameLayout by lazy { fragment.rootView as ElasticDragDismissFrameLayout }
    private val messageEntry: EditText by lazy { fragment.rootView!!.findViewById<View>(R.id.message_entry) as EditText }
    private val selectSim: View by lazy { fragment.rootView!!.findViewById<View>(R.id.select_sim) }
    private val toolbar: Toolbar by lazy { fragment.rootView!!.findViewById<Toolbar>(R.id.toolbar) }

    fun init() {
        fragment.sendManager.initSendbar()
        fragment.attachManager.setupHelperViews()
        fragment.attachInitializer.initAttachHolder()

        if (activity is BubbleActivity) {
            dragDismissFrameLayout.isEnabled = false
        }

        dragDismissFrameLayout.addListener(object : ElasticDragDismissFrameLayout.ElasticDragDismissCallback() {
            override fun onDrag(elasticOffset: Float, elasticOffsetPixels: Float, rawOffset: Float, rawOffsetPixels: Float) {}
            override fun onDragDismissed() {
                if (activity is BubbleActivity) {
                    return
                }

                fragment.dismissKeyboard()
                activity?.onBackPressed()
            }
        })

        if (messageEntry is ImageKeyboardEditText) {
            (messageEntry as ImageKeyboardEditText).setCommitContentListener(fragment.attachListener)
        }

        if (!TvUtils.hasTouchscreen(activity)) {
            messageEntry.setOnClickListener {
                (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
                        ?.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
            }
        }

        try {
            DualSimApplication(selectSim).apply(fragment.argManager.conversationId)
        } catch (e: Exception) {
        }

        toolbar.setOnClickListener {
            if (activity is MessengerActivity) {
                (activity as MessengerActivity).clickNavigationItem(R.id.drawer_view_contact)
            }
        }

        if (fragment.argManager.shouldOpenKeyboard || Build.MANUFACTURER.toLowerCase() == "blackberry") {
            // coming from the compose screen, or they have a physical keyboard (blackberry)
            messageEntry.requestFocus()
            (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
                    ?.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
            fragment.activity?.intent?.putExtra(MessengerActivityExtras.EXTRA_SHOULD_OPEN_KEYBOARD, false)
        }
    }
}