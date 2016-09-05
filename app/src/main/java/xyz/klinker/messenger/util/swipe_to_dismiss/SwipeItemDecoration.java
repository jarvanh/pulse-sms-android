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
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.Settings;

/**
 * Item decorator that shows a background on each item as it is dismissing and adds some padding
 * to the top of the first item to improve spacing.
 * <p>
 * Adapted from:
 * https://github.com/nemanja-kovacevic/recycler-view-swipe-to-delete/blob/master/app/src/main/java/net/nemanjakovacevic/recyclerviewswipetodelete/MainActivity.java
 */
public class SwipeItemDecoration extends RecyclerView.ItemDecoration {

    private Drawable background;
    private boolean initiated;

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = parent.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.top_extra_padding);
        } else if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) {
            outRect.bottom = parent.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.bottom_extra_padding);
        }
    }

    private void init(Context context) {
        background = new ColorDrawable(Settings.get(context).globalColorSet.colorLight);
        initiated = true;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {

        if (!initiated) {
            init(parent.getContext());
        }

        if (parent.getItemAnimator().isRunning()) {

            // some items might be animating down and some items might be animating up to close the
            // gap left by the removed item this is not exclusive, both movement can be happening
            // at the same time to reproduce this leave just enough items so the first one and the
            // last one would be just a little off screen then remove one from the middle

            // find first child with translationY > 0
            // and last one with translationY < 0
            // we're after a rect that is not covered in recycler-view views at this point in time
            View lastViewComingDown = null;
            View firstViewComingUp = null;

            // this we need to find out
            int left = 0;
            int right = 0;
            int top = 0;
            int bottom = 0;

            // find relevant translating views
            int childCount = parent.getLayoutManager().getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getLayoutManager().getChildAt(i);
                left = child.getLeft();
                right = child.getRight();

                if (child.getTranslationY() < 0) {
                    // view is coming down
                    lastViewComingDown = child;
                } else if (child.getTranslationY() > 0) {
                    // view is coming up
                    if (firstViewComingUp == null) {
                        firstViewComingUp = child;
                    }
                }
            }

            if (lastViewComingDown != null && firstViewComingUp != null) {
                // views are coming down AND going up to fill the void
                top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
            } else if (lastViewComingDown != null) {
                // views are going down to fill the void
                top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                bottom = lastViewComingDown.getBottom();
            } else if (firstViewComingUp != null) {
                // views are coming up to fill the void
                top = firstViewComingUp.getTop();
                bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
            }

            background.setBounds(left, top, right, bottom);
            background.draw(c);
        }

        super.onDraw(c, parent, state);
    }


}
