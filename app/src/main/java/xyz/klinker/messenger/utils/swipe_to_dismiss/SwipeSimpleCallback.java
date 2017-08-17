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

package xyz.klinker.messenger.utils.swipe_to_dismiss;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.FrameLayout;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.util.ColorUtils;

/**
 * A simple callback for a recyclerview that can act on swipe motions.
 * <p>
 * Adapted from:
 * https://github.com/nemanja-kovacevic/recycler-view-swipe-to-delete/blob/master/app/src/main/java/net/nemanjakovacevic/recyclerviewswipetodelete/MainActivity.java
 */
public class SwipeSimpleCallback extends ItemTouchHelper.SimpleCallback {

    private ConversationListAdapter adapter;
    protected Drawable endSwipeBackground;
    private Drawable startSwipeBackground;
    protected Drawable endMark; // delete icon on archive list, archive on the conversation list
    private Drawable startMark; // archive icon
    private int markMargin;
    private boolean initiated;

    protected Drawable getArchiveItem(Context context) {
        return context.getDrawable(R.drawable.ic_archive);
    }

    protected void setupEndSwipe(Context context) {
        ColorSet set = Settings.get(context).mainColorSet;
        endMark = context.getDrawable(R.drawable.ic_archive);

        if (set.colorLight == Color.WHITE) {
            endSwipeBackground = new ColorDrawable(set.colorDark);
        } else {
            endSwipeBackground = new ColorDrawable(set.colorLight);
        }

        if (ColorUtils.isColorDark(set.colorLight)) {
            endMark.setTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.deleteIcon)));
        } else {
            endMark.setTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.lightToolbarTextColor)));
        }
    }

    protected void setupStartSwipe(Context context) {
        ColorSet set = Settings.get(context).mainColorSet;
        startMark = getArchiveItem(context);

        if (set.colorLight == Color.WHITE) {
            startSwipeBackground = new ColorDrawable(set.colorDark);
        } else {
            startSwipeBackground = new ColorDrawable(set.colorLight);
        }

        if (ColorUtils.isColorDark(Settings.get(context).mainColorSet.colorLight)) {
            startMark.setTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.deleteIcon)));
        } else {
            startMark.setTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.lightToolbarTextColor)));
        }
    }

    private void init(Context context) {
        // end swipe will be delete when on the archive list, but both will be archive on the normal
        // conversation list.
        setupEndSwipe(context);
        setupStartSwipe(context);

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
        if (direction == ItemTouchHelper.START && canDelete()) {
            adapter.deleteItem(viewHolder.getAdapterPosition());
        } else {
            adapter.archiveItem(viewHolder.getAdapterPosition());
        }
    }

    private boolean canDelete() {
        return this instanceof UnarchiveSwipeSimpleCallback || this instanceof SwipeDeleteSimpleCallback;
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


        if (dX < 0) { // we are swiping towards the play (delete)
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
