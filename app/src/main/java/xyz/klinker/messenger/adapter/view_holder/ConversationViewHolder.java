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

package xyz.klinker.messenger.adapter.view_holder;

import android.animation.ValueAnimator;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.util.ConversationExpandedListener;

/**
 * View holder for recycling inflated conversations.
 */
public class ConversationViewHolder extends RecyclerView.ViewHolder {

    private static final int EXPAND_DURATION = 150;

    public TextView header;
    public CircleImageView image;
    public TextView name;
    public TextView summary;

    private boolean expanded = false;
    private int originalRecyclerHeight = -1;
    private ConversationExpandedListener listener;

    public ConversationViewHolder(View itemView, ConversationExpandedListener listener) {
        super(itemView);

        this.listener = listener;

        header = (TextView) itemView.findViewById(R.id.header);
        image = (CircleImageView) itemView.findViewById(R.id.image);
        name = (TextView) itemView.findViewById(R.id.name);
        summary = (TextView) itemView.findViewById(R.id.summary);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeExpandedState();
            }
        });
    }

    public boolean isBold() {
        return name.getTypeface() != null && name.getTypeface().isBold();
    }

    public void setBold(boolean bold) {
        if (bold) {
            name.setTypeface(Typeface.DEFAULT_BOLD);
            summary.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            name.setTypeface(Typeface.DEFAULT);
            summary.setTypeface(Typeface.DEFAULT);
        }
    }

    private void changeExpandedState() {
        if (expanded) {
            collapseConversation();
        } else {
            expandConversation();
        }
    }

    private void expandConversation() {
        expanded = true;

        animateMargins(0, itemView.getRootView().getHeight(),
                0, (int) (-1 * (itemView.getHeight() + itemView.getY())),
                new AccelerateInterpolator());

        listener.onConversationExpanded(this);
    }

    private void collapseConversation() {
        expanded = false;

        final RecyclerView recyclerView = (RecyclerView) itemView.getParent();
        animateMargins(itemView.getRootView().getHeight(), 0,
                (int) recyclerView.getTranslationY(), 0,
                new DecelerateInterpolator());

        listener.onConversationContracted(this);
    }

    private void animateMargins(final int fromBottom, final int toBottom,
                                final int startY, final int translateY,
                                Interpolator interpolator) {

        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                itemView.getLayoutParams();

        ValueAnimator animator = ValueAnimator.ofInt(fromBottom, toBottom);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                params.bottomMargin = (int) valueAnimator.getAnimatedValue();
                itemView.requestLayout();
            }
        });
        animator.setInterpolator(interpolator);
        animator.setDuration(EXPAND_DURATION);
        animator.start();

        final RecyclerView recyclerView = (RecyclerView) itemView.getParent();
        final ViewGroup.MarginLayoutParams recyclerParams = (ViewGroup.MarginLayoutParams)
                recyclerView.getLayoutParams();

        if (originalRecyclerHeight == -1) {
            originalRecyclerHeight = recyclerView.getHeight();
        }

        ValueAnimator recyclerAnimator = ValueAnimator.ofInt(startY, translateY);
        recyclerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                recyclerView.setTranslationY((int) valueAnimator.getAnimatedValue());
                recyclerParams.height = originalRecyclerHeight +
                        (-1 * (int) valueAnimator.getAnimatedValue());
                recyclerView.requestLayout();
            }
        });
        recyclerAnimator.setInterpolator(interpolator);
        recyclerAnimator.setDuration(EXPAND_DURATION);
        recyclerAnimator.start();
    }

}
