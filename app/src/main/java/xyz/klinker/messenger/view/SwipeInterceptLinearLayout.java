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

package xyz.klinker.messenger.view;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

/**
 * Linear layout that will intercept swipes from the user. This will be useful so that after a
 * message fragment is shown, the user cannot scroll the conversation list still. It will eventually
 * implement a way to swipe down or up when at the end of the view to collapse the conversation and
 * go back to the main list.
 */
public class SwipeInterceptLinearLayout extends LinearLayout {

    private static final int MAX_CHANGE = 100;

    private float initialY;

    public SwipeInterceptLinearLayout(Context context) {
        super(context);
    }

    public SwipeInterceptLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwipeInterceptLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
//        for (int i = 0; i < getChildCount(); i++) {
//            View view = getChildAt(i);
//            if (view instanceof RecyclerView) {
//                if (((LinearLayoutManager) ((RecyclerView) view).getLayoutManager())
//                        .findFirstCompletelyVisibleItemPosition() == 0) {
//                    return true;
//                }
//            }
//        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        calcTranslation(ev);
        return super.onTouchEvent(ev);
    }

    private void calcTranslation(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            initialY = ev.getY();
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            animate().translationY(calcDepreciation(ev.getY() - initialY)).setDuration(0).setListener(null);
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            setTranslationY(0);
            initialY = -1;
        }
    }

    private int calcDepreciation(float delta) {
        if (delta > MAX_CHANGE) {
            return MAX_CHANGE;
        } else if (delta < -1 * MAX_CHANGE) {
            return -1 * MAX_CHANGE;
        } else {
            return (int) (MAX_CHANGE * Math.sin(Math.PI / (2 * MAX_CHANGE) * delta));
        }
    }

}
