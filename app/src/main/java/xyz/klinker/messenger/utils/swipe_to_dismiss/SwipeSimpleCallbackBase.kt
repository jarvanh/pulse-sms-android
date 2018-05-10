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
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.widget.FrameLayout

import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.BaseSwipeAction
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.SwipeArchiveAction
import xyz.klinker.messenger.utils.swipe_to_dismiss.actions.SwipeDeleteAction

/**
 * A simple callback for a recyclerview that can act on swipe motions.
 *
 *
 * Adapted from:
 * https://github.com/nemanja-kovacevic/recycler-view-swipe-to-delete/blob/master/app/src/main/java/net/nemanjakovacevic/recyclerviewswipetodelete/MainActivity.java
 */
@Suppress("DEPRECATION")
abstract class SwipeSimpleCallbackBase(private val adapter: ConversationListAdapter) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.END) {

    private var endSwipeBackground: Drawable? = null
    private var endMark: Drawable? = null

    private var startSwipeBackground: Drawable? = null
    private var startMark: Drawable? = null

    private var markMargin: Int = 0
    private var initiated: Boolean = false

    // starting from the left side of the screen
    abstract fun getStartSwipeAction(): BaseSwipeAction

    // starting from the right side of the screen
    abstract fun getEndSwipeAction(): BaseSwipeAction

    private fun setupEndSwipe(context: Context) {
        val action = getEndSwipeAction()
        endMark = context.getDrawable(action.getIcon())
        endSwipeBackground = ColorDrawable(action.getBackgroundColor())

        if (ColorUtils.isColorDark(action.getBackgroundColor())) {
            endMark!!.setTintList(ColorStateList.valueOf(context.resources.getColor(R.color.deleteIcon)))
        } else {
            endMark!!.setTintList(ColorStateList.valueOf(context.resources.getColor(R.color.lightToolbarTextColor)))
        }
    }

    private fun setupStartSwipe(context: Context) {
        val action = getStartSwipeAction()
        startMark = context.getDrawable(action.getIcon())
        startSwipeBackground = ColorDrawable(action.getBackgroundColor())

        if (ColorUtils.isColorDark(action.getBackgroundColor())) {
            startMark!!.setTintList(ColorStateList.valueOf(context.resources.getColor(R.color.deleteIcon)))
        } else {
            startMark!!.setTintList(ColorStateList.valueOf(context.resources.getColor(R.color.lightToolbarTextColor)))
        }
    }

    private fun init(context: Context) {
        // end swipe will be delete when on the archive list, but both will be archive on the normal
        // conversation list.
        setupEndSwipe(context)
        setupStartSwipe(context)

        markMargin = context.resources.getDimension(R.dimen.delete_margin).toInt()
        initiated = true
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder) = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val action = if (direction == ItemTouchHelper.START)
            getEndSwipeAction() else getStartSwipeAction()
        action.onPerform(adapter, viewHolder.adapterPosition)
    }

    override fun getSwipeDirs(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?): Int {
        // if it is a header, don't allow swiping. if it is an item, swipe to right.
        return if (viewHolder!!.itemView is FrameLayout) 0
        else ItemTouchHelper.START or ItemTouchHelper.END // swipe TOWARDS the start or TOWARDS the end
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

        if (dX < 0) { // we are swiping towards the play (delete)
            val left = Math.min(itemView.right + dX.toInt(), itemView.right + itemView.width)

            // draw endSwipeBackground
            endSwipeBackground?.setBounds(left, itemView.top, itemView.right, itemView.bottom)
            endSwipeBackground?.draw(c)

            // draw trash can mark
            val itemHeight = itemView.bottom - itemView.top
            val intrinsicWidth = endMark!!.intrinsicWidth
            val intrinsicHeight = endMark!!.intrinsicWidth

            val xMarkLeft = itemView.right - markMargin - intrinsicWidth
            val xMarkRight = itemView.right - markMargin
            val xMarkTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val xMarkBottom = xMarkTop + intrinsicHeight
            endMark!!.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom)

            endMark!!.draw(c)
        } else { // we are swiping towards the end (archive)
            val right = Math.min(itemView.left + dX.toInt(), itemView.left + itemView.width)

            // draw background
            startSwipeBackground!!.setBounds(itemView.left, itemView.top, right, itemView.bottom)
            startSwipeBackground!!.draw(c)

            // draw trash can mark
            val itemHeight = itemView.bottom - itemView.top
            val intrinsicWidth = startMark!!.intrinsicWidth
            val intrinsicHeight = startMark!!.intrinsicWidth

            val xMarkLeft = itemView.left + markMargin
            val xMarkRight = itemView.left + markMargin + intrinsicWidth
            val xMarkTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val xMarkBottom = xMarkTop + intrinsicHeight
            startMark!!.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom)

            startMark!!.draw(c)
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

}
