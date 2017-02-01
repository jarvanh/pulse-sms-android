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

package xyz.klinker.messenger.shared.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.EdgeEffect;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.pojo.BaseTheme;

/**
 * Helper class for working with colors.
 */
public class ColorUtils {

    public static ColorSet getRandomMaterialColor(Context context) {
        int num = (int) (Math.random() * (17 + 1));

        switch (num) {
            case 0:
                return ColorSet.RED(context);
            case 1:
                return ColorSet.PINK(context);
            case 2:
                return ColorSet.PURPLE(context);
            case 3:
                return ColorSet.DEEP_PURPLE(context);
            case 4:
                return ColorSet.INDIGO(context);
            case 5:
                return ColorSet.BLUE(context);
            case 6:
                return ColorSet.LIGHT_BLUE(context);
            case 7:
                return ColorSet.CYAN(context);
            case 8:
                return ColorSet.GREEN(context);
            case 9:
                return ColorSet.LIGHT_GREEN(context);
            case 10:
                return ColorSet.LIME(context);
            case 11:
                return ColorSet.YELLOW(context);
            case 12:
                return ColorSet.AMBER(context);
            case 13:
                return ColorSet.ORANGE(context);
            case 14:
                return ColorSet.DEEP_ORANGE(context);
            case 15:
                return ColorSet.BROWN(context);
            case 16:
                return ColorSet.GREY(context);
            case 17:
                return ColorSet.BLUE_GREY(context);
            default:
                throw new RuntimeException("Invalid random color: " + num);
        }
    }

    /**
     * Converts a color integer into it's hex equivalent.
     */
    public static String convertToHex(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    /**
     * Adjusts the status bar color depending on whether you are on a phone or tablet.
     *
     * @param color    the color to change to.
     * @param activity the activity to find the views in.
     */
    public static void adjustStatusBarColor(int color, final Activity activity) {
        if (Settings.get(activity).useGlobalThemeColor) {
            color = Settings.get(activity).globalColorSet.colorDark;
        }

        if (!activity.getResources().getBoolean(R.bool.pin_drawer)) {
            final DrawerLayout drawerLayout = (DrawerLayout) activity
                    .findViewById(R.id.drawer_layout);

            if (drawerLayout != null) {
                drawerLayout.setStatusBarBackgroundColor(color);
            }
        } else {
            final View status = activity.findViewById(R.id.status_bar);

            if (status != null) {
                status.setBackgroundTintList(ColorStateList.valueOf(color));
            }
        }
    }

    /**
     * Adjusts the drawer colors and menu items to be correct depending on current state.
     *
     * @param color    the color for the header.
     * @param activity the activity to find the views in.
     */
    public static void adjustDrawerColor(int color, Activity activity) {
        adjustDrawerColor(color, false, activity);
    }

    /**
     * Adjusts the drawer colors and menu items to be correct depending on current state.
     *
     * @param color    the color for the header.
     * @param isGroup  whether we are adjusting the drawer for a group conversation or not. If so,
     *                 some of the text will be changed and the call option will be hidden.
     * @param activity the activity to find the views in.
     */
    public static void adjustDrawerColor(int color, boolean isGroup, Activity activity) {
        if (Settings.get(activity).useGlobalThemeColor) {
            color = Settings.get(activity).globalColorSet.colorDark;
        }

        final View revealView = activity.findViewById(R.id.navigation_view).findViewById(R.id.header_reveal);
        final View headerView = activity.findViewById(R.id.navigation_view).findViewById(R.id.header);
        NavigationView navView = (NavigationView) activity.findViewById(R.id.navigation_view);

        if (revealView == null) {
            return;
        }

        int cx = revealView.getMeasuredWidth() / 2;
        int cy = revealView.getMeasuredHeight() / 2;
        int radius = (int) Math.sqrt((cx * cx) + (cy * cy));

        if (revealView.getVisibility() == View.VISIBLE) {
            Animator anim =
                    ViewAnimationUtils.createCircularReveal(revealView, cx, cy, radius, 0);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    revealView.setVisibility(View.INVISIBLE);
                }
            });
            anim.setDuration(200);

            headerView.setVisibility(View.VISIBLE);
            anim.start();

            navView.getMenu().clear();
            navView.inflateMenu(R.menu.navigation_drawer_conversations);
            navView.getMenu().getItem(1).setChecked(true);

            // change the text to
            if (Account.get(activity).accountId == null && navView.getMenu().findItem(R.id.drawer_account) != null) {
                navView.getMenu().findItem(R.id.drawer_account).setTitle(R.string.menu_device_texting);
            }
        } else {
            revealView.setBackgroundColor(color);
            Animator anim =
                    ViewAnimationUtils.createCircularReveal(revealView, cx, cy, 0, radius);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    headerView.setVisibility(View.INVISIBLE);
                }
            });
            anim.setDuration(200);

            revealView.setVisibility(View.VISIBLE);
            anim.start();

            navView.getMenu().clear();

            if (isGroup) {
                navView.inflateMenu(R.menu.navigation_drawer_messages_group);
            } else {
                navView.inflateMenu(R.menu.navigation_drawer_messages);
            }
        }
    }

    /**
     * Sets the cursor color for an edit text to the supplied color. Reflection is required here,
     * unfortunately.
     *
     * @param editText the edit text to change.
     * @param color    the color of the new cursor.
     */
    public static void setCursorDrawableColor(EditText editText, int color) {
        if (Settings.get(editText.getContext()).useGlobalThemeColor) {
            color = Settings.get(editText.getContext()).globalColorSet.colorAccent;
        }

        try {
            Field fCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            fCursorDrawableRes.setAccessible(true);
            int mCursorDrawableRes = fCursorDrawableRes.getInt(editText);
            Field fEditor = TextView.class.getDeclaredField("mEditor");
            fEditor.setAccessible(true);
            Object editor = fEditor.get(editText);
            Class<?> clazz = editor.getClass();
            Field fCursorDrawable = clazz.getDeclaredField("mCursorDrawable");
            fCursorDrawable.setAccessible(true);
            Drawable[] drawables = new Drawable[2];
            drawables[0] = editText.getContext().getDrawable(mCursorDrawableRes);
            drawables[1] = editText.getContext().getDrawable(mCursorDrawableRes);
            drawables[0].setColorFilter(color, PorterDuff.Mode.SRC_IN);
            drawables[1].setColorFilter(color, PorterDuff.Mode.SRC_IN);
            fCursorDrawable.set(editor, drawables);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    /**
     * Set the color of the handles when you select text in a
     * {@link android.widget.EditText} or other view that extends {@link TextView}.
     *
     * @param view
     *     The {@link TextView} or a {@link View} that extends {@link TextView}.
     * @param color
     *     The color to set for the text handles
     */
    public static void colorTextSelectionHandles(TextView view, int color) {
        if (Settings.get(view.getContext()).useGlobalThemeColor) {
            color = Settings.get(view.getContext()).globalColorSet.colorAccent;
        }

        try {
            Field editorField = TextView.class.getDeclaredField("mEditor");
            if (!editorField.isAccessible()) {
                editorField.setAccessible(true);
            }

            Object editor = editorField.get(view);
            Class<?> editorClass = editor.getClass();

            String[] handleNames = {"mSelectHandleLeft", "mSelectHandleRight", "mSelectHandleCenter"};
            String[] resNames = {"mTextSelectHandleLeftRes", "mTextSelectHandleRightRes", "mTextSelectHandleRes"};

            for (int i = 0; i < handleNames.length; i++) {
                Field handleField = editorClass.getDeclaredField(handleNames[i]);
                if (!handleField.isAccessible()) {
                    handleField.setAccessible(true);
                }

                Drawable handleDrawable = (Drawable) handleField.get(editor);

                if (handleDrawable == null) {
                    Field resField = TextView.class.getDeclaredField(resNames[i]);
                    if (!resField.isAccessible()) {
                        resField.setAccessible(true);
                    }
                    int resId = resField.getInt(view);
                    handleDrawable = view.getResources().getDrawable(resId);
                }

                if (handleDrawable != null) {
                    Drawable drawable = handleDrawable.mutate();
                    drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                    handleField.set(editor, drawable);
                }
            }
        } catch (Exception e) {

        }
    }

    /**
     * Changes the overscroll highlight effect on a recyclerview to be the given color.
     */
    public static void changeRecyclerOverscrollColors(RecyclerView recyclerView, int color) {
        final int colorWithGlobalCalculated;
        if (Settings.get(recyclerView.getContext()).useGlobalThemeColor) {
            colorWithGlobalCalculated = Settings.get(recyclerView.getContext()).globalColorSet.color;
        } else {
            colorWithGlobalCalculated = color;
        }
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private boolean invoked = false;

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // only invoke this once
                if (invoked) {
                    return;
                } else {
                    invoked = true;
                }

                try {
                    final Class<?> clazz = RecyclerView.class;

                    for (final String name : new String[]{"ensureTopGlow", "ensureBottomGlow"}) {
                        Method method = clazz.getDeclaredMethod(name);
                        method.setAccessible(true);
                        method.invoke(recyclerView);
                    }

                    for (final String name : new String[]{"mTopGlow", "mBottomGlow"}) {
                        final Field field = clazz.getDeclaredField(name);
                        field.setAccessible(true);
                        final Object edge = field.get(recyclerView);
                        final Field fEdgeEffect = edge.getClass().getDeclaredField("mEdgeEffect");
                        fEdgeEffect.setAccessible(true);
                        ((EdgeEffect) fEdgeEffect.get(edge)).setColor(colorWithGlobalCalculated);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Changes the color of the app bar on the "recents screen", if it is not the default theme
     */
    public static void updateRecentsEntry(final Activity activity) {
        final Settings settings = Settings.get(activity);
        if (settings.useGlobalThemeColor) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                        Bitmap bm = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_launcher);
                        ActivityManager.TaskDescription td = new ActivityManager.TaskDescription(null, bm, settings.globalColorSet.color);

                        activity.setTaskDescription(td);
                }
            }).start();
        }

    }

    /**
     * Changes the window background to black if applicable
     */
    public static void checkBlackBackground(Activity activity) {
        if (Settings.get(activity).baseTheme == BaseTheme.BLACK) {
            Drawable background = activity.getWindow().getDecorView().getBackground();
            if (background instanceof ColorDrawable) {
                if (((ColorDrawable)background).getColor() == Color.BLACK) {
                    return;
                }
            }

            activity.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
    }

    public static List<ColorSet> getColors(Context context) {
        List<ColorSet> colors = new ArrayList<>();
        colors.add(ColorSet.RED(context));
        colors.add(ColorSet.PINK(context));
        colors.add(ColorSet.PURPLE(context));
        colors.add(ColorSet.DEEP_PURPLE(context));
        colors.add(ColorSet.INDIGO(context));
        colors.add(ColorSet.BLUE(context));
        colors.add(ColorSet.LIGHT_BLUE(context));
        colors.add(ColorSet.CYAN(context));
        colors.add(ColorSet.TEAL(context));
        colors.add(ColorSet.GREEN(context));
        colors.add(ColorSet.LIGHT_GREEN(context));
        colors.add(ColorSet.LIME(context));
        colors.add(ColorSet.YELLOW(context));
        colors.add(ColorSet.AMBER(context));
        colors.add(ColorSet.ORANGE(context));
        colors.add(ColorSet.DEEP_ORANGE(context));
        colors.add(ColorSet.BROWN(context));
        colors.add(ColorSet.GREY(context));
        colors.add(ColorSet.BLUE_GREY(context));
        return colors;
    }

    public static void animateToolbarColor(Activity activity, int originalColor, int newColor) {
        final ColorDrawable drawable = new ColorDrawable(originalColor);
        final ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
        actionBar.setBackgroundDrawable(drawable);

        ValueAnimator animator = ValueAnimator.ofArgb(originalColor, newColor);
        animator.setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int color = (int) valueAnimator.getAnimatedValue();
                drawable.setColor(color);
                actionBar.setBackgroundDrawable(drawable);
            }
        });
        animator.start();
    }

    public static void animateStatusBarColor(final Activity activity, int originalColor, int newColor) {
        ValueAnimator animator = ValueAnimator.ofArgb(originalColor, newColor);
        animator.setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int color = (int) valueAnimator.getAnimatedValue();
                if (activity.getWindow() != null) {
                    activity.getWindow().setStatusBarColor(color);
                }
            }
        });
        animator.start();
    }

    public static boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.30;
    }

}
