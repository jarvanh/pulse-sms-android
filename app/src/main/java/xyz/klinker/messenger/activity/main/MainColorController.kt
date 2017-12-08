package xyz.klinker.messenger.activity.main

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.TimeUtils

class MainColorController(private val activity: AppCompatActivity) {

    private val toolbar: Toolbar by lazy { activity.findViewById<View>(R.id.toolbar) as Toolbar }
    private val fab: FloatingActionButton by lazy { activity.findViewById<View>(R.id.fab) as FloatingActionButton }
    private val navigationView: NavigationView by lazy { activity.findViewById<View>(R.id.navigation_view) as NavigationView }

    fun colorActivity() {
        ColorUtils.checkBlackBackground(activity)
        ActivityUtils.setTaskDescription(activity)

        if (!Build.FINGERPRINT.contains("robolectric")) {
            TimeUtils.setupNightTheme(activity)
        }
    }

    fun configureGlobalColors() {
        if (Settings.isCurrentlyDarkTheme) {
            activity.window.navigationBarColor = Color.BLACK
        }

        toolbar.setBackgroundColor(Settings.mainColorSet.color)
        fab.backgroundTintList = ColorStateList.valueOf(Settings.mainColorSet.colorAccent)

        val states = arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked))

        val baseColor = if (activity.resources.getBoolean(R.bool.is_night)) "FFFFFF" else "000000"
        val iconColors = intArrayOf(Color.parseColor("#77" + baseColor), Settings.mainColorSet.colorAccent)
        val textColors = intArrayOf(Color.parseColor("#DD" + baseColor), Settings.mainColorSet.colorAccent)

        navigationView.itemIconTintList = ColorStateList(states, iconColors)
        navigationView.itemTextColor = ColorStateList(states, textColors)
        navigationView.post({
            ColorUtils.adjustStatusBarColor(Settings.mainColorSet.colorDark, activity)

            val header = navigationView.findViewById<View>(R.id.header)
            header?.setBackgroundColor(Settings.mainColorSet.colorDark)
        })
    }

    fun configureNavigationBarColor() {
        if (Settings.isCurrentlyDarkTheme) {
            ActivityUtils.setUpNavigationBarColor(activity, Color.BLACK)
        } else {
            ActivityUtils.setUpNavigationBarColor(activity, Color.WHITE)
        }
    }
}