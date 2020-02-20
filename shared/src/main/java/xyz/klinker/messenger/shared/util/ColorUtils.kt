/*
 * Copyright (C) 2020 Luke Klinker
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

package xyz.klinker.messenger.shared.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.EdgeEffect
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.activity.AbstractSettingsActivity
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.BaseTheme
import java.util.*


/**
 * Helper class for working with colors.
 */
object ColorUtils {

    fun getRandomMaterialColor(context: Context): ColorSet {
        val num = (Math.random() * (16 + 1)).toInt()

        when (num) {
            0 -> return ColorSet.RED(context)
            1 -> return ColorSet.PINK(context)
            2 -> return ColorSet.PURPLE(context)
            3 -> return ColorSet.DEEP_PURPLE(context)
            4 -> return ColorSet.INDIGO(context)
            5 -> return ColorSet.BLUE(context)
            6 -> return ColorSet.LIGHT_BLUE(context)
            7 -> return ColorSet.CYAN(context)
            8 -> return ColorSet.GREEN(context)
            9 -> return ColorSet.LIGHT_GREEN(context)
            10 -> return ColorSet.AMBER(context)
            11 -> return ColorSet.ORANGE(context)
            12 -> return ColorSet.DEEP_ORANGE(context)
            13 -> return ColorSet.BROWN(context)
            14 -> return ColorSet.GREY(context)
            15 -> return ColorSet.BLUE_GREY(context)
            16 -> return ColorSet.TEAL(context)
            17,
                //return ColorSet.Companion.LIME(context);
            18,
                //return ColorSet.Companion.YELLOW(context);
            19 -> return ColorSet.TEAL(context)
        //return ColorSet.Companion.WHITE(context);
            else -> return ColorSet.TEAL(context)
        }
    }

    /**
     * Converts a color integer into it's hex equivalent.
     */
    fun convertToHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    /**
     * Adjusts the status bar color depending on whether you are on a phone or tablet.
     *
     * @param color    the color to change to.
     * @param activity the activity to find the views in.
     */
    fun adjustStatusBarColor(color: Int, activity: Activity?) {
        var color = color
        if (activity == null) {
            return
        }

        if (Settings.useGlobalThemeColor) {
            color = Settings.mainColorSet.colorDark
        }

        color = ActivityUtils.possiblyOverrideColorSelection(activity, color)

        if (!activity.resources.getBoolean(R.bool.pin_drawer)) {
            val drawerLayout = activity
                    .findViewById<View>(R.id.drawer_layout) as DrawerLayout?

            drawerLayout?.setStatusBarBackgroundColor(color)
        } else {
            val status = activity.findViewById<View>(R.id.status_bar)

            if (status != null) {
                status.backgroundTintList = ColorStateList.valueOf(color)
            }
        }

        ActivityUtils.setUpLightStatusBar(activity, color)
    }

    /**
     * Adjusts the drawer colors and menu items to be correct depending on current state.
     *
     * @param color    the color for the header.
     * @param activity the activity to find the views in.
     */
    fun adjustDrawerColor(color: Int, activity: Activity?) {
        try {
            adjustDrawerColor(color, false, activity)
        } catch (e: IllegalStateException) {
        }

    }

    fun adjustDrawerColor(color: Int, isGroup: Boolean, activity: Activity?) {
        try {
            adjustDrawerColorUnsafely(color, isGroup, activity)
        } catch (e: IllegalStateException) {
        }
    }

    /**
     * Adjusts the drawer colors and menu items to be correct depending on current state.
     *
     * @param color    the color for the header.
     * @param isGroup  whether we are adjusting the drawer for a group conversation or not. If so,
     * some of the text will be changed and the call option will be hidden.
     * @param activity the activity to find the views in.
     */
    private fun adjustDrawerColorUnsafely(color: Int, isGroup: Boolean, activity: Activity?) {
        if (activity == null) {
            return
        }

        var color = color
        if (Settings.useGlobalThemeColor) {
            color = Settings.mainColorSet.colorDark
        }

        val revealView = activity.findViewById<View>(R.id.navigation_view).findViewById<View>(R.id.header_reveal)
        val headerView = activity.findViewById<View>(R.id.navigation_view).findViewById<View>(R.id.header)
        val navView = activity.findViewById<View>(R.id.navigation_view) as NavigationView

        if (revealView == null) {
            return
        }

        val cx = revealView.measuredWidth / 2
        val cy = revealView.measuredHeight / 2
        val radius = Math.sqrt((cx * cx + cy * cy).toDouble()).toInt()

        if (revealView.visibility == View.VISIBLE) {
            val anim = ViewAnimationUtils.createCircularReveal(revealView, cx, cy, radius.toFloat(), 0f)
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    revealView.visibility = View.INVISIBLE
                }
            })
            anim.duration = 200

            headerView.visibility = View.VISIBLE
            anim.start()

            navView.menu.clear()
            navView.inflateMenu(R.menu.navigation_drawer_conversations)
            navView.menu.getItem(1).isChecked = true

            // change the text to
            if (Account.accountId == null && navView.menu.findItem(R.id.drawer_account) != null) {
                navView.menu.findItem(R.id.drawer_account).setTitle(R.string.menu_device_texting)
            }

            DrawerItemHelper(navView).prepareDrawer()
        } else {
            revealView.setBackgroundColor(color)
            val anim = ViewAnimationUtils.createCircularReveal(revealView, cx, cy, 0f, radius.toFloat())
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    headerView.visibility = View.INVISIBLE
                }
            })
            anim.duration = 200

            revealView.visibility = View.VISIBLE
            anim.start()

            navView.menu.clear()

            if (isGroup) {
                navView.inflateMenu(R.menu.navigation_drawer_messages_group)
            } else {
                navView.inflateMenu(R.menu.navigation_drawer_messages)
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
    fun setCursorDrawableColor(editText: EditText, color: Int) {
        var color = color
        if (Settings.useGlobalThemeColor) {
            color = Settings.mainColorSet.colorAccent
        }

        try {
            val fCursorDrawableRes = TextView::class.java.getDeclaredField("mCursorDrawableRes")
            fCursorDrawableRes.isAccessible = true
            val mCursorDrawableRes = fCursorDrawableRes.getInt(editText)
            val fEditor = TextView::class.java.getDeclaredField("mEditor")
            fEditor.isAccessible = true
            val editor = fEditor.get(editText)
            val clazz = editor.javaClass
            val fCursorDrawable = clazz.getDeclaredField("mCursorDrawable")
            fCursorDrawable.isAccessible = true
            val drawables = arrayOfNulls<Drawable>(2)
            drawables[0] = editText.context.getDrawable(mCursorDrawableRes)
            drawables[1] = editText.context.getDrawable(mCursorDrawableRes)
            drawables[0]?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            drawables[1]?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            fCursorDrawable.set(editor, drawables)
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }

    }

    /**
     * Set the color of the handles when you select text in a
     * [android.widget.EditText] or other view that extends [TextView].
     *
     * @param view
     * The [TextView] or a [View] that extends [TextView].
     * @param color
     * The color to set for the text handles
     */
    fun colorTextSelectionHandles(view: TextView, color: Int) {
        if (AndroidVersionUtil.isAndroidP) {
            return
        }

        var color = color
        if (Settings.useGlobalThemeColor) {
            color = Settings.mainColorSet.colorAccent
        }

        try {
            val editorField = TextView::class.java.getDeclaredField("mEditor")
            if (!editorField.isAccessible) {
                editorField.isAccessible = true
            }

            val editor = editorField.get(view)
            val editorClass = editor.javaClass

            val handleNames = arrayOf("mSelectHandleLeft", "mSelectHandleRight", "mSelectHandleCenter")
            val resNames = arrayOf("mTextSelectHandleLeftRes", "mTextSelectHandleRightRes", "mTextSelectHandleRes")

            for (i in handleNames.indices) {
                val handleField = editorClass.getDeclaredField(handleNames[i])
                if (!handleField.isAccessible) {
                    handleField.isAccessible = true
                }

                var handleDrawable: Drawable? = handleField.get(editor) as Drawable?

                if (handleDrawable == null) {
                    val resField = TextView::class.java.getDeclaredField(resNames[i])
                    if (!resField.isAccessible) {
                        resField.isAccessible = true
                    }
                    val resId = resField.getInt(view)
                    handleDrawable = view.resources.getDrawable(resId)
                }

                if (handleDrawable != null) {
                    val drawable = handleDrawable.mutate()
                    drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                    handleField.set(editor, drawable)
                }
            }
        } catch (e: Exception) {
        }

    }

    /**
     * Changes the overscroll highlight effect on a recyclerview to be the given color.
     */
    fun changeRecyclerOverscrollColors(recyclerView: RecyclerView, color: Int) {
        val colorWithGlobalCalculated = if (Settings.useGlobalThemeColor) {
            Settings.mainColorSet.color
        } else {
            color
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var invoked = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                // only invoke this once
                if (invoked) {
                    return
                } else {
                    invoked = true
                }

                val clazz = RecyclerView::class.java

                try {
                    for (name in arrayOf("ensureTopGlow", "ensureBottomGlow", "ensureLeftGlow", "ensureRightGlow")) {
                        val method = clazz.getDeclaredMethod(name)
                        method.isAccessible = true
                        method.invoke(recyclerView)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    for (name in arrayOf("mTopGlow", "mBottomGlow", "mLeftGlow", "mRightGlow")) {
                        val field = clazz.getDeclaredField(name)
                        field.isAccessible = true
                        val edge = field.get(recyclerView)
                        (edge as EdgeEffect).color = colorWithGlobalCalculated
                    }
                } catch (e: Exception) {
                    e.printStackTrace()

                    try {
                        for (name in arrayOf("mTopGlow", "mBottomGlow", "mLeftGlow", "mRightGlow")) {
                            val field = clazz.getDeclaredField(name)
                            field.isAccessible = true
                            val edge = field.get(recyclerView)
                            val fEdgeEffect = edge.javaClass.getDeclaredField("mEdgeEffect")
                            fEdgeEffect.isAccessible = true
                            (edge as EdgeEffect).color = colorWithGlobalCalculated
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }

                }

            }
        })
    }

    /**
     * Changes the window background to black if applicable
     */
    fun checkBlackBackground(activity: Activity) {
        if (Settings.baseTheme === BaseTheme.BLACK) {
            val background = activity.window.decorView.background
            if (background is ColorDrawable) {
                if (background.color == Color.BLACK) {
                    return
                }
            }

            activity.window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        }
    }

    fun getColors(context: Context): List<ColorSet> {
        val colors = ArrayList<ColorSet>()
        colors.add(ColorSet.RED(context))
        colors.add(ColorSet.PINK(context))
        colors.add(ColorSet.PURPLE(context))
        colors.add(ColorSet.DEEP_PURPLE(context))
        colors.add(ColorSet.INDIGO(context))
        colors.add(ColorSet.DEFAULT(context))
        colors.add(ColorSet.BLUE(context))
        colors.add(ColorSet.LIGHT_BLUE(context))
        colors.add(ColorSet.CYAN(context))
        colors.add(ColorSet.TEAL(context))
        colors.add(ColorSet.GREEN(context))
        colors.add(ColorSet.LIGHT_GREEN(context))
        colors.add(ColorSet.LIME(context))
        colors.add(ColorSet.YELLOW(context))
        colors.add(ColorSet.AMBER(context))
        colors.add(ColorSet.ORANGE(context))
        colors.add(ColorSet.DEEP_ORANGE(context))
//        colors.add(ColorSet.BROWN(context))
        colors.add(ColorSet.GREY(context))
        colors.add(ColorSet.BLUE_GREY(context))
        colors.add(ColorSet.WHITE(context))
        return colors
    }

    fun animateToolbarColor(activity: Activity, originalColor: Int, newColor: Int) {
        val drawable = ColorDrawable(originalColor)
        var actionBar: ActionBar? = null
        var toolbar: Toolbar? = null

        try {
            if (activity is AbstractSettingsActivity) {
                toolbar = activity.toolbar
                toolbar!!.setBackgroundColor(originalColor)
            } else {
                actionBar = (activity as AppCompatActivity).supportActionBar
                actionBar!!.setBackgroundDrawable(drawable)
            }
        } catch (e: Exception) {
        }

        val animator = ValueAnimator.ofArgb(originalColor, newColor)
        animator.duration = 200
        animator.addUpdateListener { valueAnimator ->
            val color = valueAnimator.animatedValue as Int
            if (toolbar != null) {
                toolbar.setBackgroundColor(color)
            } else {
                drawable.color = color
                actionBar?.setBackgroundDrawable(drawable)
            }
        }

        animator.start()
    }

    fun animateStatusBarColor(activity: Activity, originalColor: Int, newColor: Int) {
        val animator = ValueAnimator.ofArgb(originalColor, newColor)
        animator.duration = 200
        animator.addUpdateListener { valueAnimator ->
            val color = valueAnimator.animatedValue as Int
            if (activity.window != null) {
                ActivityUtils.setStatusBarColor(activity, color)
            }
        }
        animator.start()
    }

    fun isColorDark(color: Int): Boolean {
        val red = getSRGB(Color.red(color))
        val green = getSRGB(Color.green(color))
        val blue = getSRGB(Color.blue(color))

        // Compute the relative luminance of the background color
        // https://www.w3.org/TR/WCAG20/#relativeluminancedef
        val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue

        // Determine color based on the contrast ratio 4.5:1
        // https://www.w3.org/TR/WCAG20/#contrast-ratiodef
        return luminance < 0.233
    }

    private fun getSRGB(value: Int): Double {
        val component = value.toDouble() / 255.0
        return if (component <= 0.03928) component / 12.92 else Math.pow(((component + 0.055) / 1.055), 2.4)
    }
}
