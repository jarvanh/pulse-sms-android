/*
 * Copyright (C) 2016 Jacob Klinker
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

package xyz.klinker.messenger.util.swipe_to_dismiss;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.FrameLayout;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.data.Settings;

/**
 * A simple callback for a recyclerview that can act on swipe motions.
 * <p>
 * Adapted from:
 * https://github.com/nemanja-kovacevic/recycler-view-swipe-to-delete/blob/master/app/src/main/java/net/nemanjakovacevic/recyclerviewswipetodelete/MainActivity.java
 */
public class SwipeSimpleCallback extends ItemTouchHelper.SimpleCallback {

    private ConversationListAdapter adapter;
    private Drawable endSwipeBackground;
    private Drawable startSwipeBackground;
    private Drawable endMark; // delete icon
    private Drawable startMark; // archive icon
    private int markMargin;
    private boolean initiated;

    protected Drawable getArchiveItem(Context context) {
        return context.getDrawable(R.drawable.ic_archive);
    }

    private void init(Context context) {
        endSwipeBackground = new ColorDrawable(Settings.get(context).globalColorSet.colorLight);
        endMark = context.getDrawable(R.drawable.ic_delete_sweep);
        endMark.setColorFilter(context.getResources().getColor(R.color.deleteIcon), PorterDuff.Mode.SRC_ATOP);

        Settings settings = Settings.get(context);
        if (settings.useGlobalThemeColor) {
            startSwipeBackground = new ColorDrawable(settings.globalColorSet.colorAccent);
        } else {
            startSwipeBackground = new ColorDrawable(context.getResources().getColor(R.color.colorAccent));
        }

        startMark = getArchiveItem(context);
        startMark.setColorFilter(context.getResources().getColor(R.color.deleteIcon), PorterDuff.Mode.SRC_ATOP);

        markMargin = (int) context.getResources().getDimension(R.dimen.delete_margin);
        initiated = true;
    }

    public SwipeSimpleCallback(ConversationListAdapter adapter) {
        super(0, ItemTouchHelper.END);
        this.adapter = adapter;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        if (direction == ItemTouchHelper.START) {
            adapter.deleteItem(viewHolder.getAdapterPosition());
        } else {
            adapter.archiveItem(viewHolder.getAdapterPosition());
        }
    }

    @Override
    public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        // if it is a header, don't allow swiping. if it is an item, swipe to right.
        if (viewHolder.itemView instanceof FrameLayout) {
            return 0;
        } else {
            // swipe TOWARDS the start or TOWARDS the end
            return ItemTouchHelper.START | ItemTouchHelper.END;
        }
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;

        // not sure why, but this method get's called for viewholder that are already swiped away
        if (viewHolder.getAdapterPosition() == -1) {
            return;
        }

        if (!initiated) {
            init(recyclerView.getContext());
        }


        if (dX < 0) { // we are swiping towards the start (delete)
            int left = Math.min(itemView.getRight() + (int) dX,
                    itemView.getRight() + itemView.getWidth());

            // draw endSwipeBackground
            endSwipeBackground.setBounds(left, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            endSwipeBackground.draw(c);

            // draw trash can mark
            int itemHeight = itemView.getBottom() - itemView.getTop();
            int intrinsicWidth = endMark.getIntrinsicWidth();
            int intrinsicHeight = endMark.getIntrinsicWidth();

            int xMarkLeft = itemView.getRight() - markMargin - intrinsicWidth;
            int xMarkRight = itemView.getRight() - markMargin;
            int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int xMarkBottom = xMarkTop + intrinsicHeight;
            endMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);

            endMark.draw(c);
        } else { // we are swiping towards the end (archive)
            int right = Math.min(itemView.getLeft() + (int) dX,
                    itemView.getLeft() + itemView.getWidth());

            // draw background
            startSwipeBackground.setBounds(itemView.getLeft(), itemView.getTop(), right, itemView.getBottom());
            startSwipeBackground.draw(c);

            // draw trash can mark
            int itemHeight = itemView.getBottom() - itemView.getTop();
            int intrinsicWidth = startMark.getIntrinsicWidth();
            int intrinsicHeight = startMark.getIntrinsicWidth();

            int xMarkLeft = itemView.getLeft() + markMargin;
            int xMarkRight = itemView.getLeft() + markMargin + intrinsicWidth;
            int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int xMarkBottom = xMarkTop + intrinsicHeight;
            startMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);

            startMark.draw(c);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

}
