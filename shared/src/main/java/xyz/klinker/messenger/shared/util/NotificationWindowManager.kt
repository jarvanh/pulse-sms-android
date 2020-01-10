/*
 * Copyright (C) 2020 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.shared.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.service.notification.NotificationService

/**
 * Does the heavy lifting behind setting up the window correctly so that the service can draw
 * over other applications.
 */
class NotificationWindowManager(val service: NotificationService) {

    var windowManager: WindowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val windowParams: WindowManager.LayoutParams
    private val handler: Handler = Handler()
    private val upcomingQueue = mutableListOf<View>()

    private var isRemoving: Boolean = false
    var currentView: View? = null
        private set

    init {
        this.isRemoving = false
        this.windowParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        )

        windowParams.gravity = Gravity.BOTTOM or Gravity.END
    }

    /**
     * Adds a view to the service and displays it on the screen.
     * @param view the view to display.
     */
    fun addOverlayView(view: View?) {
        if (view != null) {
            if (currentView != null) {
                upcomingQueue.add(view)

                if (!isRemoving) {
                    removeOverlayView(currentView)
                }
            } else {
                windowManager!!.addView(view, windowParams)

                scheduleDismissal(view)

                // delay slightly to give the view time to layout. this way values will not
                // be zero for y and height.
                view.visibility = View.INVISIBLE
                handler.postDelayed({
                    val y = view.y
                    view.y = y + view.height
                    view.visibility = View.VISIBLE
                    view.animate()
                            .y(y)
                            .setInterpolator(FastOutSlowInInterpolator())
                            .setDuration(ANIMATION_DURATION.toLong())
                            .start()

                    val icon = view.findViewById<View>(R.id.icon)
                    icon.alpha = 0.0f
                    icon.animate()
                            .alpha(1.0f)
                            .setDuration(ANIMATION_DURATION.toLong())
                            .start()

                    val text = view.findViewById<View>(R.id.text)
                    text.alpha = 0.0f
                    text.animate()
                            .alpha(1.0f)
                            .setDuration(ANIMATION_DURATION.toLong())
                            .start()
                }, ANIMATION_DELAY.toLong())
                currentView = view
            }
        }
    }

    /**
     * Removes a view from the service and hides it from the screen.
     * @param view the view to hide.
     */
    fun removeOverlayView(view: View?) {
        if (view != null) {
            isRemoving = true
            view.animate()
                    .y(view.y + view.height)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .setDuration(ANIMATION_DURATION.toLong())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            view.visibility = View.INVISIBLE

                            currentView = null
                            windowManager!!.removeView(view)
                            isRemoving = false

                            checkQueue()
                        }
                    })
                    .start()
        }
    }

    /**
     * Schedule the view to be removed after the allotted amount of time for NOTIFICATION_TIME
     * @param view the view scheduled for dismissal.
     */
    private fun scheduleDismissal(view: View?) {
        handler.postDelayed({
            if (view === currentView) {
                removeOverlayView(currentView)
            } else {
                scheduleDismissal(currentView)
            }
        }, NOTIFICATION_TIME.toLong())
    }

    /**
     * Checks to see if there is anything in the queue that still needs displayed.
     */
    fun checkQueue() {
        if (upcomingQueue.size > 0) {
            val nextView = upcomingQueue.removeAt(0)
            addOverlayView(nextView)
        }
    }

    companion object {
        private val TAG = "NotificationManager"
        private val NOTIFICATION_TIME = 8000
        private val ANIMATION_DURATION = 500
        private val ANIMATION_DELAY = 50
    }

}
