/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.widget.FrameLayout
import xyz.klinker.messenger.R

/**
 * A [FrameLayout] which responds to nested scrolls to create drag-dismissable layouts.
 * Applies an elasticity factor to reduce movement as you approach the given dismiss distance.
 * Optionally also scales down content during drag.
 *
 * https://github.com/nickbutcher/plaid/blob/master/app/src/main/java/io/plaidapp/ui/widget/ElasticDragDismissFrameLayout.java
 */
class ElasticDragDismissFrameLayout : FrameLayout {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    // configurable attribs
    private var dragDismissDistance = java.lang.Float.MAX_VALUE
    private val dragDismissFraction = -1f
    private val dragDismissScale = 1f
    private var shouldScale = false
    private val dragElasticity = 0.5f

    // state
    private var totalDrag: Float = 0.toFloat()
    private var draggingDown = false
    private var draggingUp = false
    private var lastEvent: Int? = null

    private var enabled = true
    private var callbacks = mutableListOf<ElasticDragDismissCallback>()

    val isDragging: Boolean
        get() = draggingDown || draggingUp

    init {
        dragDismissDistance = resources
                .getDimensionPixelSize(R.dimen.drag_down_dismiss_distance).toFloat()
        shouldScale = dragDismissScale != 1f
    }

    abstract class ElasticDragDismissCallback {

        /**
         * Called for each drag event.
         *
         * @param elasticOffset       Indicating the drag offset with elasticity applied i.e. may
         * exceed 1.
         * @param elasticOffsetPixels The elastically scaled drag distance in pixels.
         * @param rawOffset           Value from [0, 1] indicating the raw drag offset i.e.
         * without elasticity applied. A value of 1 indicates that the
         * dismiss distance has been reached.
         * @param rawOffsetPixels     The raw distance the user has dragged
         */
        open fun onDrag(elasticOffset: Float, elasticOffsetPixels: Float,
                        rawOffset: Float, rawOffsetPixels: Float) {
        }

        /**
         * Called when dragging is released and has exceeded the threshold dismiss distance.
         */
        open fun onDragDismissed() {}

    }

    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        return if (enabled) {
            nestedScrollAxes and View.SCROLL_AXIS_VERTICAL != 0
        } else {
            false
        }
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        if (enabled) {
            // if we're in a drag gesture and the user reverses up the we should take those events
            if (draggingDown && dy > 0 || draggingUp && dy < 0) {
                dragScale(dy)
                consumed[1] = dy
            }
        }
    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int,
                                dxUnconsumed: Int, dyUnconsumed: Int) {
        if (enabled) {
            dragScale(dyUnconsumed)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        lastEvent = ev?.action
        return super.onInterceptTouchEvent(ev)
    }

    override fun onStopNestedScroll(child: View) {
        if (enabled) {
            if (Math.abs(totalDrag) >= dragDismissDistance) {
                dispatchDismissCallback()
            } else { // settle back to natural position
                if (fastOutSlowInInterpolator == null) {
                    fastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context,
                            android.R.interpolator.fast_out_slow_in)
                }

                if (lastEvent == MotionEvent.ACTION_DOWN) {
                    // this is a 'defensive cleanup for new gestures',
                    // don't animate here
                    // see also https://github.com/nickbutcher/plaid/issues/185
                    translationY = 0f
                    scaleX = 1f
                    scaleY = 1f
                } else {
                    animate()
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200L)
                            .setInterpolator(fastOutSlowInInterpolator)
                            .setListener(null)
                            .start()
                }

                totalDrag = 0f
                draggingUp = false
                draggingDown = draggingUp
                dispatchDragCallback(0f, 0f, 0f, 0f)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (dragDismissFraction > 0f) {
            dragDismissDistance = h * dragDismissFraction
        }
    }

    fun addListener(listener: ElasticDragDismissCallback) {
        callbacks.add(listener)
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun isEnabled(): Boolean {
        return enabled
    }

    private fun dragScale(scroll: Int) {
        if (scroll == 0) return

        totalDrag += scroll.toFloat()

        // track the direction & set the pivot point for scaling
        // don't double track i.e. if play dragging down and then reverse, keep tracking as
        // dragging down until they reach the 'natural' position
        if (scroll < 0 && !draggingUp && !draggingDown) {
            draggingDown = true
            if (shouldScale) pivotY = height.toFloat()
        } else if (scroll > 0 && !draggingDown && !draggingUp) {
            draggingUp = true
            if (shouldScale) pivotY = 0f
        }
        // how far have we dragged relative to the distance to perform a dismiss
        // (0–1 where 1 = dismiss distance). Decreasing logarithmically as we approach the limit
        var dragFraction = Math.log10((1 + Math.abs(totalDrag) / dragDismissDistance).toDouble()).toFloat()

        // calculate the desired translation given the drag fraction
        var dragTo = dragFraction * dragDismissDistance * dragElasticity

        if (draggingUp) {
            // as we use the absolute magnitude when calculating the drag fraction, need to
            // re-apply the drag direction
            dragTo *= -1f
        }
        translationY = dragTo

        if (shouldScale) {
            val scale = 1 - (1 - dragDismissScale) * dragFraction
            scaleX = scale
            scaleY = scale
        }

        // if we've reversed direction and gone past the settle point then clear the flags to
        // allow the list to get the scroll events & reset any transforms
        if (draggingDown && totalDrag >= 0 || draggingUp && totalDrag <= 0) {
            dragFraction = 0f
            dragTo = dragFraction
            totalDrag = dragTo
            draggingUp = false
            draggingDown = draggingUp
            translationY = 0f
            scaleX = 1f
            scaleY = 1f
        }
        dispatchDragCallback(dragFraction, dragTo,
                Math.min(1f, Math.abs(totalDrag) / dragDismissDistance), totalDrag)
    }

    private fun dispatchDragCallback(elasticOffset: Float, elasticOffsetPixels: Float,
                                     rawOffset: Float, rawOffsetPixels: Float) {
        if (!callbacks.isEmpty()) {
            for (callback in callbacks) {
                callback.onDrag(elasticOffset, elasticOffsetPixels, rawOffset, rawOffsetPixels)
            }
        }
    }

    private fun dispatchDismissCallback() {
        if (!callbacks.isEmpty()) {
            for (callback in callbacks) {
                callback.onDragDismissed()
            }
        }
    }

    companion object {
        private var fastOutSlowInInterpolator: Interpolator? = null
    }
}