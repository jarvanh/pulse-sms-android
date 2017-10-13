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

package xyz.klinker.messenger.shared.data

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.util.ColorConverter

/**
 * Holds 4 different theme colors: the primary color, a darker and lighter version of the primary
 * color and an accent color.
 */
class ColorSet {

    var color: Int = 0
    var colorDark: Int = 0
    var colorLight: Int = 0
    var colorAccent: Int = 0

    constructor()
    private constructor(context: Context, @ColorRes color: Int, @ColorRes colorDark: Int,
                        @ColorRes colorLight: Int, @ColorRes colorAccent: Int) {
        this.color = getColor(context, color)
        this.colorDark = getColor(context, colorDark)
        this.colorLight = getColor(context, colorLight)
        this.colorAccent = getColor(context, colorAccent)
    }

    private fun getColor(context: Context?, @ColorRes color: Int): Int {
        if (context == null) {
            return Color.WHITE
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getColor(color)
            } else {
                context.resources.getColor(color)
            }
        } catch (e: Exception) {
            Color.WHITE
        }

    }

    companion object {
        private var defaultSet: ColorSet? = null
        fun DEFAULT(context: Context): ColorSet {
            if (defaultSet == null) {
                defaultSet = ColorSet(context, R.color.colorPrimary, R.color.colorPrimaryDark,
                        R.color.colorPrimaryLight, R.color.colorAccent)
            }

            return defaultSet!!
        }

        private var redSet: ColorSet? = null
        fun RED(context: Context): ColorSet {
            if (redSet == null) {
                redSet = ColorSet(context, R.color.materialRed, R.color.materialRedDark,
                        R.color.materialRedLight, R.color.materialIndigoAccent)
            }

            return redSet!!
        }

        private var pinkSet: ColorSet? = null
        fun PINK(context: Context): ColorSet {
            if (pinkSet == null) {
                pinkSet = ColorSet(context, R.color.materialPink, R.color.materialPinkDark,
                        R.color.materialPinkLight, R.color.materialLimeAccent)
            }

            return pinkSet!!
        }

        private var purpleSet: ColorSet? = null
        fun PURPLE(context: Context): ColorSet {
            if (purpleSet == null) {
                purpleSet = ColorSet(context, R.color.materialPurple, R.color.materialPurpleDark,
                        R.color.materialPurpleLight, R.color.materialTealAccent)
            }

            return purpleSet!!
        }

        private var deepPurpleSet: ColorSet? = null
        fun DEEP_PURPLE(context: Context): ColorSet {
            if (deepPurpleSet == null) {
                deepPurpleSet = ColorSet(context, R.color.materialDeepPurple, R.color.materialDeepPurpleDark,
                        R.color.materialDeepPurpleLight, R.color.materialPinkAccent)
            }

            return deepPurpleSet!!
        }

        private var indigoSet: ColorSet? = null
        fun INDIGO(context: Context): ColorSet {
            if (indigoSet == null) {
                indigoSet = ColorSet(context, R.color.materialIndigo, R.color.materialIndigoDark,
                        R.color.materialIndigoLight, R.color.materialYellowAccent)
            }

            return indigoSet!!
        }

        private var blueSet: ColorSet? = null
        fun BLUE(context: Context): ColorSet {
            if (blueSet == null) {
                blueSet = ColorSet(context, R.color.materialBlue, R.color.materialBlueDark,
                        R.color.materialBlueLight, R.color.materialDeepOrangeAccent)
            }

            return blueSet!!
        }

        private var lightBlueSet: ColorSet? = null
        fun LIGHT_BLUE(context: Context): ColorSet {
            if (lightBlueSet == null) {
                lightBlueSet = ColorSet(context, R.color.materialLightBlue, R.color.materialLightBlueDark,
                        R.color.materialLightBlueLight, R.color.materialPurpleAccent)
            }

            return lightBlueSet!!
        }

        private var cyanSet: ColorSet? = null
        fun CYAN(context: Context): ColorSet {
            if (cyanSet == null) {
                cyanSet = ColorSet(context, R.color.materialCyan, R.color.materialCyanDark,
                        R.color.materialCyanLight, R.color.materialAmberAccent)
            }

            return cyanSet!!
        }

        private var tealSet: ColorSet? = null
        fun TEAL(context: Context): ColorSet {
            if (tealSet == null) {
                tealSet = ColorSet(context, R.color.materialTeal, R.color.materialTealDark,
                        R.color.materialTealLight, R.color.materialOrangeAccent)
            }

            return tealSet!!
        }

        private var greenSet: ColorSet? = null
        fun GREEN(context: Context): ColorSet {
            if (greenSet == null) {
                greenSet = ColorSet(context, R.color.materialGreen, R.color.materialGreenDark,
                        R.color.materialGreenLight, R.color.materialLightBlueAccent)
            }

            return greenSet!!
        }

        private var lightGreenSet: ColorSet? = null
        fun LIGHT_GREEN(context: Context): ColorSet {
            if (lightGreenSet == null) {
                lightGreenSet = ColorSet(context, R.color.materialLightGreen, R.color.materialLightGreenDark,
                        R.color.materialLightGreenLight, R.color.materialOrangeAccent)
            }

            return lightGreenSet!!
        }

        private var limeSet: ColorSet? = null
        fun LIME(context: Context): ColorSet {
            if (limeSet == null) {
                limeSet = ColorSet(context, R.color.materialLime, R.color.materialLimeDark,
                        R.color.materialLimeLight, R.color.materialBlueAccent)
            }

            return limeSet!!
        }

        private var yellowSet: ColorSet? = null
        fun YELLOW(context: Context): ColorSet {
            if (yellowSet == null) {
                yellowSet = ColorSet(context, R.color.materialYellow, R.color.materialYellowDark,
                        R.color.materialYellowLight, R.color.materialRedAccent)
            }

            return yellowSet!!
        }

        private var amberSet: ColorSet? = null
        fun AMBER(context: Context): ColorSet {
            if (amberSet == null) {
                amberSet = ColorSet(context, R.color.materialAmber, R.color.materialAmberDark,
                        R.color.materialAmberLight, R.color.materialCyanAccent)
            }

            return amberSet!!
        }

        private var orangeSet: ColorSet? = null
        fun ORANGE(context: Context): ColorSet {
            if (orangeSet == null) {
                orangeSet = ColorSet(context, R.color.materialOrange, R.color.materialOrangeDark,
                        R.color.materialOrangeLight, R.color.materialDeepPurpleAccent)
            }

            return orangeSet!!
        }

        private var deepOrangeSet: ColorSet? = null
        fun DEEP_ORANGE(context: Context): ColorSet {
            if (deepOrangeSet == null) {
                deepOrangeSet = ColorSet(context, R.color.materialDeepOrange, R.color.materialDeepOrangeDark,
                        R.color.materialDeepOrangeLight, R.color.materialLightGreenAccent)
            }

            return deepOrangeSet!!
        }

        private var brownSet: ColorSet? = null
        fun BROWN(context: Context): ColorSet {
            if (brownSet == null) {
                brownSet = ColorSet(context, R.color.materialBrown, R.color.materialBrownDark,
                        R.color.materialBrownLight, R.color.materialOrangeAccent)
            }
            return brownSet!!
        }

        private var greySet: ColorSet? = null
        fun GREY(context: Context): ColorSet {
            if (greySet == null) {
                greySet = ColorSet(context, R.color.materialGrey, R.color.materialGreyDark,
                        R.color.materialGreyLight, R.color.materialGreenAccent)
            }

            return greySet!!
        }

        private var blueGreySet: ColorSet? = null
        fun BLUE_GREY(context: Context): ColorSet {
            if (blueGreySet == null) {
                blueGreySet = ColorSet(context, R.color.materialBlueGrey, R.color.materialBlueGreyDark,
                        R.color.materialBlueGreyLight, R.color.materialRedAccent)
            }

            return blueGreySet!!
        }

        private var blackSet: ColorSet? = null
        fun BLACK(context: Context): ColorSet {
            if (blackSet == null) {
                blackSet = ColorSet(context, android.R.color.black, android.R.color.black,
                        android.R.color.black, R.color.materialTealAccent)
            }

            return blackSet!!
        }

        private var whiteSet: ColorSet? = null
        fun WHITE(context: Context): ColorSet {
            if (whiteSet == null) {
                whiteSet = ColorSet(context, android.R.color.white, R.color.materialWhiteDark,
                        android.R.color.white, R.color.materialOrangeAccent)
            }

            return whiteSet!!
        }

        fun create(@ColorInt primary: Int, @ColorInt primaryDark: Int, @ColorInt accentColor: Int): ColorSet {
            val set = ColorSet()
            set.color = primary
            set.colorDark = primaryDark
            set.colorLight = ColorConverter.lightenPrimaryColor(primary)
            set.colorAccent = accentColor

            return set
        }

        fun getFromString(context: Context, colorString: String): ColorSet {
            when (colorString) {
                "red" -> return RED(context)
                "pink" -> return PINK(context)
                "purple" -> return PURPLE(context)
                "deep_purple" -> return DEEP_PURPLE(context)
                "indigo" -> return INDIGO(context)
                "blue" -> return BLUE(context)
                "light_blue" -> return LIGHT_BLUE(context)
                "cyan" -> return CYAN(context)
                "teal" -> return TEAL(context)
                "green" -> return GREEN(context)
                "light_green" -> return LIGHT_GREEN(context)
                "lime" -> return LIME(context)
                "yellow" -> return YELLOW(context)
                "amber" -> return AMBER(context)
                "orange" -> return ORANGE(context)
                "deep_orange" -> return DEEP_ORANGE(context)
                "brown" -> return BROWN(context)
                "gray" -> return GREY(context)
                "blue_gray" -> return BLUE_GREY(context)
                "black" -> return BLACK(context)
                else -> return DEFAULT(context)
            }
        }
    }
}