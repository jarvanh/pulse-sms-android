package xyz.klinker.messenger.fragment.message

import android.graphics.Rect
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import xyz.klinker.android.drag_dismiss.util.StatusBarHelper
import xyz.klinker.messenger.activity.MessengerActivity
import java.lang.Exception
import java.lang.IllegalStateException

class EdgeToEdgeKeyboardWorkaround(private val activity: MessengerActivity) {
    private val contentContainer = activity.findViewById<ViewGroup>(android.R.id.content)
    private val rootView = contentContainer.getChildAt(0)
    private val rootViewLayout = rootView.layoutParams as FrameLayout.LayoutParams
    private var viewTreeObserver = rootView.viewTreeObserver
    private val listener = ViewTreeObserver.OnGlobalLayoutListener { possiblyResizeChildOfContent() }

    private val contentAreaOfWindowBounds = Rect()
    private var usableHeighPrevious = 0

    fun addListener() {
        try {
            viewTreeObserver.addOnGlobalLayoutListener(listener)
        } catch (e: IllegalStateException) {
            // observer isn't alive, get it again
            viewTreeObserver = rootView.viewTreeObserver
            viewTreeObserver.addOnGlobalLayoutListener(listener)
        }
    }

    fun removeListener() {
        try {
            viewTreeObserver.removeOnGlobalLayoutListener(listener)
        } catch (e: IllegalStateException) {
            // observer isn't alive, get it again
            viewTreeObserver = rootView.viewTreeObserver
            viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    private fun possiblyResizeChildOfContent() {
        contentContainer.getWindowVisibleDisplayFrame(contentAreaOfWindowBounds)
        val usableHeightNow = contentAreaOfWindowBounds.height() + StatusBarHelper.getStatusBarHeight(activity) + activity.insetController.bottomInsetValue
        if (usableHeightNow != usableHeighPrevious) {
            rootViewLayout.height = usableHeightNow
            rootView.layout(contentAreaOfWindowBounds.left, contentAreaOfWindowBounds.top, contentAreaOfWindowBounds.right, contentAreaOfWindowBounds.bottom)
            rootView.requestLayout()
            usableHeighPrevious = usableHeightNow
        }
    }
}