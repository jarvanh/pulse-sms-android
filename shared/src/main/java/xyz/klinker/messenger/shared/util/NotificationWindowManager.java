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

package xyz.klinker.messenger.shared.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.service.NotificationService;

/**
 * Does the heavy lifting behind setting up the window correctly so that the service can draw
 * over other applications.
 */
public class NotificationWindowManager {

    private static final String TAG = "NotificationManager";
    private static final int NOTIFICATION_TIME = 8000;
    private static final int ANIMATION_DURATION = 500;
    private static final int ANIMATION_DELAY = 50;

    private NotificationService service;
    private WindowManager manager;
    private WindowManager.LayoutParams params;
    private Handler handler;
    private View currentView;
    private List<View> upcomingQueue;
    private boolean isRemoving;

    /**
     * Initialize a new instance of NotificationWindowManager.
     * @param service the service to find the window from.
     */
    public NotificationWindowManager(NotificationService service) {
        this.service = service;
        this.manager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
        this.handler = new Handler();
        this.upcomingQueue = new ArrayList<View>();
        this.isRemoving = false;

        this.params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.BOTTOM | Gravity.END;
    }

    /**
     * Adds a view to the service and displays it on the screen.
     * @param view the view to display.
     */
    public void addOverlayView(final View view) {
        if (view != null) {
            if (currentView != null) {
                upcomingQueue.add(view);

                if (!isRemoving) {
                    removeOverlayView(currentView);
                }
            } else {
                manager.addView(view, params);

                scheduleDismissal(view);

                // delay slightly to give the view time to layout. this way values will not
                // be zero for y and height.
                view.setVisibility(View.INVISIBLE);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        float y = view.getY();
                        view.setY(y + view.getHeight());
                        view.setVisibility(View.VISIBLE);
                        view.animate()
                                .y(y)
                                .setInterpolator(new FastOutSlowInInterpolator())
                                .setDuration(ANIMATION_DURATION)
                                .start();

                        View icon = view.findViewById(R.id.icon);
                        icon.setAlpha(0.0f);
                        icon.animate()
                                .alpha(1.0f)
                                .setDuration(ANIMATION_DURATION)
                                .start();

                        View text = view.findViewById(R.id.text);
                        text.setAlpha(0.0f);
                        text.animate()
                                .alpha(1.0f)
                                .setDuration(ANIMATION_DURATION)
                                .start();
                    }
                }, ANIMATION_DELAY);
                currentView = view;
            }
        }
    }

    /**
     * Removes a view from the service and hides it from the screen.
     * @param view the view to hide.
     */
    public void removeOverlayView(final View view) {
        if (view != null) {
            isRemoving = true;
            view.animate()
                    .y(view.getY() + view.getHeight())
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .setDuration(ANIMATION_DURATION)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            view.setVisibility(View.INVISIBLE);

                            currentView = null;
                            manager.removeView(view);
                            isRemoving = false;

                            checkQueue();
                        }
                    })
                    .start();
        }
    }

    /**
     * Schedule the view to be removed after the allotted amount of time for NOTIFICATION_TIME
     * @param view the view scheduled for dismissal.
     */
    private void scheduleDismissal(final View view) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (view == currentView) {
                    removeOverlayView(currentView);
                } else {
                    scheduleDismissal(currentView);
                }
            }
        }, NOTIFICATION_TIME);
    }

    /**
     * Checks to see if there is anything in the queue that still needs displayed.
     */
    public void checkQueue() {
        if (upcomingQueue.size() > 0) {
            View nextView = upcomingQueue.remove(0);
            addOverlayView(nextView);
        }
    }

    /**
     * Sets the window manager.
     * @param manager the new window manager.
     */
    public void setWindowManager(WindowManager manager) {
        this.manager = manager;
    }

    /**
     * Gets the window manager.
     * @return the window manager.
     */
    public WindowManager getWindowManager() {
        return manager;
    }

    /**
     * Gets the layout params used to add a view.
     * @return the layout params.
     */
    public WindowManager.LayoutParams getWindowParams() {
        return params;
    }

    /**
     * Gets the currently shown notification.
     * @return the notification.
     */
    public View getCurrentView() {
        return currentView;
    }

    /**
     * Gets the upcoming queued notifications.
     * @return the queue.
     */
    public List<View> getQueue() {
        return upcomingQueue;
    }

    /**
     * Gets the notification service that this manager is attached to.
     * @return the service.
     */
    public Context getContext() {
        return service;
    }

}
