/*
 * Copyright (C) 2017 Luke Klinker
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

import android.animation.ValueAnimator
import android.app.Activity
import android.content.res.Resources
import android.support.design.widget.FloatingActionButton
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator

import xyz.klinker.android.drag_dismiss.util.StatusBarHelper
import xyz.klinker.messenger.shared.R

/**
 * Helper for handling all animations such as expanding and contracting conversations so that we
 * can keep those classes cleaner.
 */
object AnimationUtils {

    val EXPAND_CONVERSATION_DURATION = 250
    val CONTRACT_CONVERSATION_DURATION = 175

    private val EXPAND_PERIPHERAL_DURATION = EXPAND_CONVERSATION_DURATION
    private val CONTRACT_PERIPHERAL_DURATION = CONTRACT_CONVERSATION_DURATION

    var toolbarSize = Integer.MIN_VALUE
    var conversationListSize = Integer.MIN_VALUE

    /**
     * Animates a lines item to the full height of the view.
     *
     * @param itemView the item to animate.
     */
    fun expandConversationListItem(itemView: View) {
        PerformanceProfiler.logEvent("expanding conversation item")

        val extraExpand = itemView.resources
                .getDimensionPixelSize(R.dimen.extra_expand_distance)
        animateConversationListItem(itemView, 0, itemView.rootView.height,
                0, (-1 * (itemView.height.toFloat() + itemView.y + extraExpand.toFloat())).toInt(),
                FastOutLinearInInterpolator(), EXPAND_CONVERSATION_DURATION)

        val recyclerView = itemView.parent as RecyclerView
        recyclerView.animate().alpha(0f)
                .setDuration((EXPAND_CONVERSATION_DURATION / 5).toLong())
                .setInterpolator(FastOutLinearInInterpolator())
                .start()
    }

    /**
     * Animates a line item back to its original height.
     *
     * @param itemView the item to animate.
     */
    fun contractConversationListItem(itemView: View) {
        PerformanceProfiler.logEvent("contracting conversation item")

        if (itemView.parent !is RecyclerView) {
            return
        }

        val recyclerView = itemView.parent as RecyclerView
        val realScreenHeight = Resources.getSystem().displayMetrics.heightPixels
        val recyclerHeight = itemView.rootView.height
        val percentDifferent = (realScreenHeight.toDouble() - recyclerHeight.toDouble()) / realScreenHeight.toDouble()
        val heightToUse = if (percentDifferent > .25) realScreenHeight else recyclerHeight

        AnimationUtils.animateConversationListItem(itemView, heightToUse, 0,
                recyclerView.translationY.toInt(), 0,
                DecelerateInterpolator(), CONTRACT_CONVERSATION_DURATION)

        recyclerView.animate().alpha(1f)
                .setStartDelay(100)
                .setDuration(EXPAND_CONVERSATION_DURATION.toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
    }

    /**
     * Animates a conversation list item from the ConversationListAdapter from the bottom so that
     * it fills the whole screen, giving the impression that the conversation is coming out of this
     * item.
     *
     * @param itemView     the view holder item view.
     * @param fromBottom   the starting bottom margin for the view.
     * @param toBottom     the ending bottom margin for the view.
     * @param startY       the starting y position for the view.
     * @param translateY   the amount to move the view in the Y direction.
     * @param interpolator the interpolator to animate with.
     */
    private fun animateConversationListItem(itemView: View,
                                            fromBottom: Int, toBottom: Int,
                                            startY: Int, translateY: Int,
                                            interpolator: Interpolator, duration: Int) {
        val params = itemView.layoutParams as RecyclerView.LayoutParams

        val animator = ValueAnimator.ofInt(fromBottom, toBottom)
        animator.addUpdateListener { valueAnimator ->
            params.bottomMargin = valueAnimator.animatedValue as Int
            itemView.invalidate()
        }
        animator.interpolator = interpolator
        animator.duration = duration.toLong()
        animator.start()

        val recyclerView = itemView.parent as RecyclerView
        val recyclerParams = recyclerView.layoutParams as ViewGroup.MarginLayoutParams

        val activity = itemView.context as Activity

        if (conversationListSize == Integer.MIN_VALUE) {
            toolbarSize = activity.findViewById<View>(R.id.toolbar).height
            conversationListSize = activity.findViewById<View>(R.id.content).height
        }

        val realScreenHeight = Resources.getSystem().displayMetrics.heightPixels
        val percentDifferent = (realScreenHeight.toDouble() - AnimationUtils.conversationListSize.toDouble()) / realScreenHeight.toDouble()
        val originalHeight = (if (Math.abs(percentDifferent) > .25)
            realScreenHeight - StatusBarHelper.getStatusBarHeight(activity)
        else
            AnimationUtils.conversationListSize) - toolbarSize

        val recyclerAnimator = ValueAnimator.ofInt(startY, translateY)
        recyclerAnimator.addUpdateListener { valueAnimator ->
            recyclerView.translationY = (valueAnimator.animatedValue as Int).toFloat()
            recyclerParams.height = originalHeight + -1 * valueAnimator.animatedValue as Int
            recyclerView.requestLayout()
        }
        recyclerAnimator.interpolator = interpolator
        recyclerAnimator.duration = duration.toLong()
        recyclerAnimator.start()
    }

    /**
     * Expands the activity peripherals to be off of the screen so that the new conversation can
     * fill this entire space instead. This includes 3 different pieces:
     *
     *
     * 1. Raise the toolbar off the top of the screen.
     * 2. Raise the fragment container to the top of the screen and expand the height so that it
     * stays matching the bottom.
     * 3. Lower the FAB off the bottom.
     *
     * @param activity the activity to find the views at.
     */
    fun expandActivityForConversation(activity: Activity) {
        val toolbar = activity.findViewById<View>(R.id.app_bar_layout)
        val fragmentContainer = activity.findViewById<View>(R.id.conversation_list_container)
        val fab = activity.findViewById<View>(R.id.fab) as FloatingActionButton

        activity.findViewById<View?>(R.id.nav_bar_divider)?.visibility = View.GONE

        toolbar.postDelayed({
            activity.findViewById<View?>(R.id.conversation_list_container)?.setBackgroundColor(activity.resources.getColor(R.color.drawerBackground))
        }, EXPAND_CONVERSATION_DURATION + 50L)

        val extraDistance = activity.resources
                .getDimensionPixelSize(R.dimen.extra_expand_distance)
        val toolbarTranslate = -1 * (toolbar.height + extraDistance)
        val fabTranslate = fab.height + extraDistance +
                activity.resources.getDimensionPixelSize(R.dimen.fab_margin)

        animateActivityWithConversation(toolbar, fragmentContainer, fab,
                toolbarTranslate, 0, toolbarTranslate, fabTranslate,
                FastOutLinearInInterpolator(), EXPAND_PERIPHERAL_DURATION)
        fab.hide()
    }

    /**
     * Contracts the activity so that the original peripherals are shown again. This includes 3
     * pieces:
     *
     *
     * 1. Lower the toolbar back to it's original spot under the status bar
     * 2. Lower the top of the fragment container to under the toolbar and contract it's height so
     * that it stays matching the bottom.
     * 3. Raise the FAB back onto the screen.
     *
     * @param activity the activity to find the views in.
     */
    fun contractActivityFromConversation(activity: Activity?) {
        if (activity == null) {
            return
        }

        val toolbar = activity.findViewById<View>(R.id.app_bar_layout)
        val fragmentContainer = activity.findViewById<View>(R.id.conversation_list_container)
        val fab = activity.findViewById<View>(R.id.fab) as FloatingActionButton

        activity.findViewById<View?>(R.id.nav_bar_divider)?.visibility = View.VISIBLE
        activity.findViewById<View?>(R.id.conversation_list_container)?.setBackgroundColor(activity.resources.getColor(R.color.background))

        animateActivityWithConversation(toolbar, fragmentContainer, fab, 0,
                fragmentContainer.translationY.toInt(), 0, 0,
                FastOutLinearInInterpolator(), CONTRACT_PERIPHERAL_DURATION)
        fab.show()
    }

    /**
     * Animates peripheral items on the screen to a given ending point.
     *
     * @param toolbar            the toolbar to animate.
     * @param fragmentContainer  the fragment container to animate.
     * @param fab                the floating action button to animate.
     * @param toolbarTranslate   the distance to translate the toolbar.
     * @param containerStart     the play point of the container.
     * @param containerTranslate the distance the container should translate.
     * @param fabTranslate       the distance the fab should translate.
     * @param interpolator       the interpolator to use.
     */
    private fun animateActivityWithConversation(toolbar: View, fragmentContainer: View,
                                                fab: View, toolbarTranslate: Int,
                                                containerStart: Int, containerTranslate: Int,
                                                fabTranslate: Int,
                                                interpolator: Interpolator, duration: Int) {
        toolbar.animate().withLayer().translationY(toolbarTranslate.toFloat())
                .setDuration(duration.toLong())
                .setInterpolator(interpolator)
                .setListener(null)

        val containerParams = fragmentContainer.layoutParams as ViewGroup.MarginLayoutParams

        val activity = fragmentContainer.context as Activity

        if (conversationListSize == Integer.MIN_VALUE) {
            toolbarSize = activity.findViewById<View>(R.id.toolbar).height
            conversationListSize = activity.findViewById<View>(R.id.content).height
        }

        val realScreenHeight = Resources.getSystem().displayMetrics.heightPixels
        val percentDifferent = (realScreenHeight.toDouble() - AnimationUtils.conversationListSize.toDouble()) / realScreenHeight.toDouble()
        val originalHeight = (if (Math.abs(percentDifferent) > .25)
            realScreenHeight - StatusBarHelper.getStatusBarHeight(activity)
        else
            AnimationUtils.conversationListSize) - toolbarSize

        val containerAnimator = ValueAnimator.ofInt(containerStart, containerTranslate)
        containerAnimator.addUpdateListener { valueAnimator ->
            fragmentContainer.translationY = (valueAnimator.animatedValue as Int).toFloat()
            containerParams.height = originalHeight + -1 * valueAnimator.animatedValue as Int
            fragmentContainer.requestLayout()
        }
        containerAnimator.interpolator = interpolator
        containerAnimator.duration = duration.toLong()
        containerAnimator.start()
    }

    /**
     * Animates peripherals onto the screen when opening a conversation. This includes the
     * sendbar and the toolbar. The message list will be animated separately after the
     * list has been loaded from the database.
     *
     * @param view the view to be animated in.
     */
    fun animateConversationPeripheralIn(view: View) {
        val animator = view.animate().withLayer().alpha(1f)

        if (TvUtils.hasTouchscreen(view.context)) {
            animator.translationY(0f)
        } else {
            view.translationY = 0f
        }

        animator.setDuration(EXPAND_CONVERSATION_DURATION.toLong())
                .setInterpolator(DecelerateInterpolator())
                .setListener(null)
    }

}
