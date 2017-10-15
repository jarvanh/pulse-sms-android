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

package xyz.klinker.messenger.utils.swipe_to_dismiss

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.view.View

import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.BaseTheme

@Suppress("DEPRECATION")
/**
 * Item decorator that shows a background on each item as it is dismissing and adds some padding
 * to the top of the first item to improve spacing.
 *
 *
 * Adapted from:
 * https://github.com/nemanja-kovacevic/recycler-view-swipe-to-delete/blob/master/app/src/main/java/net/nemanjakovacevic/recyclerviewswipetodelete/MainActivity.java
 */
class SwipeItemDecoration : RecyclerView.ItemDecoration() {

    private var background: Drawable? = null
    private var initiated: Boolean = false

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                state: RecyclerView.State?) {
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = parent.context.resources
                    .getDimensionPixelSize(R.dimen.top_extra_padding)
        } else if (parent.getChildAdapterPosition(view) == parent.adapter.itemCount - 1) {
            outRect.bottom = parent.context.resources
                    .getDimensionPixelSize(R.dimen.bottom_extra_padding)
        }
    }

    private fun init(context: Context) {
        background = if (Settings.baseTheme === BaseTheme.BLACK) {
            ColorDrawable(Color.BLACK)
        } else {
            ColorDrawable(context.resources.getColor(R.color.swipeBackground))
        }
        initiated = true
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {

        if (!initiated) {
            init(parent.context)
        }

        if (parent.itemAnimator.isRunning) {

            // some items might be animating down and some items might be animating up to close the
            // gap left by the removed item this is not exclusive, both movement can be happening
            // at the same time to reproduce this leave just enough items so the first one and the
            // last one would be just a little off screen then remove one from the middle

            // find first child with translationY > 0
            // and last one with translationY < 0
            // we're after a rect that is not covered in recycler-view views at this point in time
            var lastViewComingDown: View? = null
            var firstViewComingUp: View? = null

            // this we need to find out
            var left = 0
            var right = 0
            var top = 0
            var bottom = 0

            // find relevant translating views
            val childCount = parent.layoutManager.childCount
            for (i in 0 until childCount) {
                val child = parent.layoutManager.getChildAt(i)
                left = child.left
                right = child.right

                if (child.translationY < 0) {
                    // view is coming down
                    lastViewComingDown = child
                } else if (child.translationY > 0) {
                    // view is coming up
                    if (firstViewComingUp == null) {
                        firstViewComingUp = child
                    }
                }
            }

            if (lastViewComingDown != null && firstViewComingUp != null) {
                // views are coming down AND going up to fill the void
                top = lastViewComingDown.bottom + lastViewComingDown.translationY.toInt()
                bottom = firstViewComingUp.top + firstViewComingUp.translationY.toInt()
            } else if (lastViewComingDown != null) {
                // views are going down to fill the void
                top = lastViewComingDown.bottom + lastViewComingDown.translationY.toInt()
                bottom = lastViewComingDown.bottom
            } else if (firstViewComingUp != null) {
                // views are coming up to fill the void
                top = firstViewComingUp.top
                bottom = firstViewComingUp.top + firstViewComingUp.translationY.toInt()
            }

            background!!.setBounds(left, top, right, bottom)
            background!!.draw(c)
        }

        super.onDraw(c, parent, state)
    }


}
