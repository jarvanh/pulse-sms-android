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

package xyz.klinker.messenger.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Field;

import xyz.klinker.messenger.R;

/**
 * Helper class for working with colors.
 */
public class ColorUtil {

    public static int getRandomMaterialColor(Context context) {
        int num = (int) (Math.random() * (17 + 1));

        switch (num) {
            case 0:     return context.getResources().getColor(R.color.materialRed);
            case 1:     return context.getResources().getColor(R.color.materialPink);
            case 2:     return context.getResources().getColor(R.color.materialPurple);
            case 3:     return context.getResources().getColor(R.color.materialDeepPurple);
            case 4:     return context.getResources().getColor(R.color.materialIndigo);
            case 5:     return context.getResources().getColor(R.color.materialBlue);
            case 6:     return context.getResources().getColor(R.color.materialLightBlue);
            case 7:     return context.getResources().getColor(R.color.materialCyan);
            case 8:     return context.getResources().getColor(R.color.materialGreen);
            case 9:     return context.getResources().getColor(R.color.materialLightGreen);
            case 10:    return context.getResources().getColor(R.color.materialLime);
            case 11:    return context.getResources().getColor(R.color.materialYellow);
            case 12:    return context.getResources().getColor(R.color.materialAmber);
            case 13:    return context.getResources().getColor(R.color.materialOrange);
            case 14:    return context.getResources().getColor(R.color.materialDeepOrange);
            case 15:    return context.getResources().getColor(R.color.materialBrown);
            case 16:    return context.getResources().getColor(R.color.materialGrey);
            case 17:    return context.getResources().getColor(R.color.materialBlueGrey);
            default:    throw new RuntimeException("Invalid random color: " + num);
        }
    }

    /**
     * Adjusts the status bar color depending on whether you are on a phone or tablet.
     *
     * @param color the color to change to.
     * @param activity the activity to find the views in.
     */
    public static void adjustStatusBarColor(final int color, final Activity activity) {
        if (!activity.getResources().getBoolean(R.bool.pin_drawer)) {
            final DrawerLayout drawerLayout = (DrawerLayout) activity
                    .findViewById(R.id.drawer_layout);
            drawerLayout.setStatusBarBackgroundColor(color);
        } else {
            activity.findViewById(R.id.status_bar)
                    .setBackgroundTintList(ColorStateList.valueOf(color));
        }


    }

    /**
     * Adjusts the drawer colors and menu items to be correct depending on current state.
     *
     * @param color the color for the header.
     * @param activity the activity to find the views in.
     */
    public static void adjustDrawerColor(int color, Activity activity) {
        final View revealView = activity.findViewById(R.id.header_reveal);
        final View headerView = activity.findViewById(R.id.header);
        NavigationView navView = (NavigationView) activity.findViewById(R.id.navigation_view);

        int cx = revealView.getMeasuredWidth() / 2;
        int cy = revealView.getMeasuredHeight() / 2;
        int radius = (int) Math.sqrt((cx*cx) + (cy*cy));

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
            navView.getMenu().getItem(0).setChecked(true);
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
            navView.inflateMenu(R.menu.navigation_drawer_messages);
        }
    }

    /**
     * Sets the cursor color for an edit text to the supplied color. Reflection is required here,
     * unfortunately.
     *
     * @param editText the edit text to change.
     * @param color the color of the new cursor.
     */
    public static void setCursorDrawableColor(EditText editText, int color) {
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
            drawables[0] = editText.getContext().getResources().getDrawable(mCursorDrawableRes);
            drawables[1] = editText.getContext().getResources().getDrawable(mCursorDrawableRes);
            drawables[0].setColorFilter(color, PorterDuff.Mode.SRC_IN);
            drawables[1].setColorFilter(color, PorterDuff.Mode.SRC_IN);
            fCursorDrawable.set(editor, drawables);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

}
