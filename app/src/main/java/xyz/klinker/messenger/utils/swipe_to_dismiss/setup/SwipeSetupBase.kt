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

package xyz.klinker.messenger.utils.swipe_to_dismiss.setup

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.FrameLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.BaseSwipeAction
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.SwipeNoAction

/**
 * A simple callback for a recyclerview that can act on swipe motions.
 *
 *
 * Adapted from:
 * https://github.com/nemanja-kovacevic/recycler-view-swipe-to-delete/blob/master/app/src/main/java/net/nemanjakovacevic/recyclerviewswipetodelete/MainActivity.java
 */
@Suppress("DEPRECATION")
abstract class SwipeSetupBase(private val adapter: ConversationListAdapter) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.END) {

    private companion object {
        private const val LEFT_TO_RIGHT = ItemTouchHelper.END
        private const val RIGHT_TO_LEFT = ItemTouchHelper.START
    }

    private var leftToRightBackground: Drawable? = null
    private var leftToRightIcon: Drawable? = null

    private var rightToLeftBackground: Drawable? = null
    private var rightToLeftIcon: Drawable? = null

    private var iconMargin: Int = 0
    private var initiated: Boolean = false

    // starting from the left side of the screen
    abstract fun getLeftToRightAction(): BaseSwipeAction

    // starting from the right side of the screen
    abstract fun getRightToLeftAction(): BaseSwipeAction

    private fun setupLeftToRightSwipe(context: Context) {
        val action = getLeftToRightAction()
        leftToRightIcon = context.getDrawable(action.getIcon())
        leftToRightBackground = ColorDrawable(action.getBackgroundColor())

        if (ColorUtils.isColorDark(action.getBackgroundColor())) {
            leftToRightIcon!!.setTintList(ColorStateList.valueOf(context.resources.getColor(R.color.deleteIcon)))
        } else {
            leftToRightIcon!!.setTintList(ColorStateList.valueOf(context.resources.getColor(R.color.lightToolbarTextColor)))
        }
    }

    private fun setupRightToLeftSwipe(context: Context) {
        val action = getRightToLeftAction()
        rightToLeftIcon = context.getDrawable(action.getIcon())
        rightToLeftBackground = ColorDrawable(action.getBackgroundColor())

        if (ColorUtils.isColorDark(action.getBackgroundColor())) {
            rightToLeftIcon!!.setTintList(ColorStateList.valueOf(context.resources.getColor(R.color.deleteIcon)))
        } else {
            rightToLeftIcon!!.setTintList(ColorStateList.valueOf(context.resources.getColor(R.color.lightToolbarTextColor)))
        }
    }

    private fun init(context: Context) {
        // end swipe will be delete when on the archive list, but both will be archive on the normal
        // conversation list.
        setupRightToLeftSwipe(context)
        setupLeftToRightSwipe(context)

        iconMargin = context.resources.getDimension(R.dimen.delete_margin).toInt()
        initiated = true
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder) = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val action = if (direction == ItemTouchHelper.START)
            getRightToLeftAction() else getLeftToRightAction()
        action.onPerform(adapter, viewHolder.adapterPosition)
    }

    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return when {
            viewHolder.itemView is FrameLayout -> 0 // header view
            getRightToLeftAction() !is SwipeNoAction && getLeftToRightAction() !is SwipeNoAction -> RIGHT_TO_LEFT or LEFT_TO_RIGHT
            getRightToLeftAction() !is SwipeNoAction -> RIGHT_TO_LEFT
            getLeftToRightAction() !is SwipeNoAction -> LEFT_TO_RIGHT
            else -> 0
        }
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                             dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        val itemView = viewHolder.itemView

        // not sure why, but this method get's called for viewholder that are already swiped away
        if (viewHolder.adapterPosition == -1) {
            return
        }

        if (!initiated) {
            init(recyclerView.context)
        }

        if (dX < 0) {
            val left = Math.min(itemView.right + dX.toInt(), itemView.right + itemView.width)

            rightToLeftBackground?.setBounds(left, itemView.top, itemView.right, itemView.bottom)
            rightToLeftBackground?.draw(c)

            val itemHeight = itemView.bottom - itemView.top
            val intrinsicWidth = rightToLeftIcon!!.intrinsicWidth
            val intrinsicHeight = rightToLeftIcon!!.intrinsicWidth

            val xMarkLeft = itemView.right - iconMargin - intrinsicWidth
            val xMarkRight = itemView.right - iconMargin
            val xMarkTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val xMarkBottom = xMarkTop + intrinsicHeight

            rightToLeftIcon?.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom)
            rightToLeftIcon?.draw(c)
        } else {
            val right = Math.min(itemView.left + dX.toInt(), itemView.left + itemView.width)

            leftToRightBackground!!.setBounds(itemView.left, itemView.top, right, itemView.bottom)
            leftToRightBackground!!.draw(c)

            val itemHeight = itemView.bottom - itemView.top
            val intrinsicWidth = leftToRightIcon!!.intrinsicWidth
            val intrinsicHeight = leftToRightIcon!!.intrinsicWidth

            val xMarkLeft = itemView.left + iconMargin
            val xMarkRight = itemView.left + iconMargin + intrinsicWidth
            val xMarkTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val xMarkBottom = xMarkTop + intrinsicHeight

            leftToRightIcon?.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom)
            leftToRightIcon?.draw(c)
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}


