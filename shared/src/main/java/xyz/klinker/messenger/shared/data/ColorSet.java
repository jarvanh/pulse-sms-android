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

package xyz.klinker.messenger.shared.data;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.util.ColorConverter;

/**
 * Holds 4 different theme colors: the primary color, a darker and lighter version of the primary
 * color and an accent color.
 */
public class ColorSet {

    public int color;
    public int colorDark;
    public int colorLight;
    public int colorAccent;

    private ColorSet(Context context, @ColorRes int color, @ColorRes int colorDark,
                     @ColorRes int colorLight, @ColorRes int colorAccent) {
        this.color = getColor(context, color);
        this.colorDark = getColor(context, colorDark);
        this.colorLight = getColor(context, colorLight);
        this.colorAccent = getColor(context, colorAccent);
    }

    public ColorSet() {

    }

    private int getColor(Context context, @ColorRes int color) {
        if (context == null) {
            return Color.WHITE;
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return context.getColor(color);
            } else {
                return context.getResources().getColor(color);
            }
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    private static ColorSet defaultSet = null;
    public static ColorSet DEFAULT(Context context) {
        if (defaultSet == null) {
            defaultSet = new ColorSet(context, R.color.colorPrimary, R.color.colorPrimaryDark,
                    R.color.colorPrimaryLight, R.color.colorAccent);
        }

        return defaultSet;
    }

    private static ColorSet redSet = null;
    public static ColorSet RED(Context context) {
        if (redSet == null) {
            redSet = new ColorSet(context, R.color.materialRed, R.color.materialRedDark,
                    R.color.materialRedLight, R.color.materialIndigoAccent);
        }

        return redSet;
    }

    private static ColorSet pinkSet = null;
    public static ColorSet PINK(Context context) {
        if (pinkSet == null) {
            pinkSet = new ColorSet(context, R.color.materialPink, R.color.materialPinkDark,
                    R.color.materialPinkLight, R.color.materialLimeAccent);
        }

        return pinkSet;
    }

    private static ColorSet purpleSet = null;
    public static ColorSet PURPLE(Context context) {
        if (purpleSet == null) {
            purpleSet = new ColorSet(context, R.color.materialPurple, R.color.materialPurpleDark,
                    R.color.materialPurpleLight, R.color.materialTealAccent);
        }

        return purpleSet;
    }

    private static ColorSet deepPurpleSet = null;
    public static ColorSet DEEP_PURPLE(Context context) {
        if (deepPurpleSet == null) {
            deepPurpleSet = new ColorSet(context, R.color.materialDeepPurple, R.color.materialDeepPurpleDark,
                    R.color.materialDeepPurpleLight, R.color.materialPinkAccent);
        }

        return deepPurpleSet;
    }

    private static ColorSet indigoSet = null;
    public static ColorSet INDIGO(Context context) {
        if (indigoSet == null) {
            indigoSet = new ColorSet(context, R.color.materialIndigo, R.color.materialIndigoDark,
                    R.color.materialIndigoLight, R.color.materialYellowAccent);
        }

        return indigoSet;
    }

    private static ColorSet blueSet = null;
    public static ColorSet BLUE(Context context) {
        if (blueSet == null) {
            blueSet = new ColorSet(context, R.color.materialBlue, R.color.materialBlueDark,
                    R.color.materialBlueLight, R.color.materialDeepOrangeAccent);
        }

        return blueSet;
    }

    private static ColorSet lightBlueSet = null;
    public static ColorSet LIGHT_BLUE(Context context) {
        if (lightBlueSet == null) {
            lightBlueSet = new ColorSet(context, R.color.materialLightBlue, R.color.materialLightBlueDark,
                    R.color.materialLightBlueLight, R.color.materialPurpleAccent);
        }

        return lightBlueSet;
    }

    private static ColorSet cyanSet = null;
    public static ColorSet CYAN(Context context) {
        if (cyanSet == null) {
            cyanSet = new ColorSet(context, R.color.materialCyan, R.color.materialCyanDark,
                    R.color.materialCyanLight, R.color.materialAmberAccent);
        }

        return cyanSet;
    }

    private static ColorSet tealSet = null;
    public static ColorSet TEAL(Context context) {
        if (tealSet == null) {
            tealSet = new ColorSet(context, R.color.materialTeal, R.color.materialTealDark,
                    R.color.materialTealLight, R.color.materialPinkAccent);
        }

        return tealSet;
    }

    private static ColorSet greenSet = null;
    public static ColorSet GREEN(Context context) {
        if (greenSet == null) {
            greenSet = new ColorSet(context, R.color.materialGreen, R.color.materialGreenDark,
                    R.color.materialGreenLight, R.color.materialLightBlueAccent);
        }

        return greenSet;
    }

    private static ColorSet lightGreenSet = null;
    public static ColorSet LIGHT_GREEN(Context context) {
        if (lightGreenSet == null) {
            lightGreenSet = new ColorSet(context, R.color.materialLightGreen, R.color.materialLightGreenDark,
                    R.color.materialLightGreenLight, R.color.materialOrangeAccent);
        }

        return lightGreenSet;
    }

    private static ColorSet limeSet = null;
    public static ColorSet LIME(Context context) {
        if (limeSet == null) {
            limeSet = new ColorSet(context, R.color.materialLime, R.color.materialLimeDark,
                    R.color.materialLimeLight, R.color.materialBlueAccent);
        }

        return limeSet;
    }

    private static ColorSet yellowSet = null;
    public static ColorSet YELLOW(Context context) {
        if (yellowSet == null) {
            yellowSet = new ColorSet(context, R.color.materialYellow, R.color.materialYellowDark,
                    R.color.materialYellowLight, R.color.materialRedAccent);
        }

        return yellowSet;
    }

    private static ColorSet amberSet = null;
    public static ColorSet AMBER(Context context) {
        if (amberSet == null) {
            amberSet = new ColorSet(context, R.color.materialAmber, R.color.materialAmberDark,
                    R.color.materialAmberLight, R.color.materialCyanAccent);
        }

        return amberSet;
    }

    private static ColorSet orangeSet = null;
    public static ColorSet ORANGE(Context context) {
        if (orangeSet == null) {
            orangeSet = new ColorSet(context, R.color.materialOrange, R.color.materialOrangeDark,
                    R.color.materialOrangeLight, R.color.materialDeepPurpleAccent);
        }

        return orangeSet;
    }

    private static ColorSet deepOrangeSet = null;
    public static ColorSet DEEP_ORANGE(Context context) {
        if (deepOrangeSet == null) {
            deepOrangeSet = new ColorSet(context, R.color.materialDeepOrange, R.color.materialDeepOrangeDark,
                    R.color.materialDeepOrangeLight, R.color.materialLightGreenAccent);
        }

        return deepOrangeSet;
    }

    private static ColorSet brownSet = null;
    public static ColorSet BROWN(Context context) {
        if (brownSet == null) {
            brownSet = new ColorSet(context, R.color.materialBrown, R.color.materialBrownDark,
                    R.color.materialBrownLight, R.color.materialOrangeAccent);
        }
        return brownSet;
    }

    private static ColorSet greySet = null;
    public static ColorSet GREY(Context context) {
        if (greySet == null) {
            greySet = new ColorSet(context, R.color.materialGrey, R.color.materialGreyDark,
                    R.color.materialGreyLight, R.color.materialGreenAccent);
        }

        return greySet;
    }

    private static ColorSet blueGreySet = null;
    public static ColorSet BLUE_GREY(Context context) {
        if (blueGreySet == null) {
            blueGreySet = new ColorSet(context, R.color.materialBlueGrey, R.color.materialBlueGreyDark,
                    R.color.materialBlueGreyLight, R.color.materialRedAccent);
        }

        return blueGreySet;
    }

    private static ColorSet blackSet = null;
    public static ColorSet BLACK(Context context) {
        if (blackSet == null) {
            blackSet = new ColorSet(context, android.R.color.black, android.R.color.black,
                    android.R.color.black, R.color.materialTealAccent);
        }

        return blackSet;
    }

    private static ColorSet whiteSet = null;
    public static ColorSet WHITE(Context context) {
        if (whiteSet == null) {
            whiteSet = new ColorSet(context, android.R.color.white, R.color.materialWhiteDark,
                    android.R.color.white, R.color.materialOrangeAccent);
        }

        return whiteSet;
    }

    public static ColorSet create(@ColorInt int primary, @ColorInt int primaryDark, @ColorInt int accentColor) {
        ColorSet set = new ColorSet();
        set.color = primary;
        set.colorDark = primaryDark;
        set.colorLight = ColorConverter.lightenPrimaryColor(primary);
        set.colorAccent = accentColor;

        return set;
    }

    public static ColorSet getFromString(Context context, String colorString) {
        switch (colorString) {
            case "red":
                return RED(context);
            case "pink":
                return PINK(context);
            case "purple":
                return PURPLE(context);
            case "deep_purple":
                return DEEP_PURPLE(context);
            case "indigo":
                return INDIGO(context);
            case "blue":
                return BLUE(context);
            case "light_blue":
                return LIGHT_BLUE(context);
            case "cyan":
                return CYAN(context);
            case "teal":
                return TEAL(context);
            case "green":
                return GREEN(context);
            case  "light_green":
                return LIGHT_GREEN(context);
            case "lime":
                return LIME(context);
            case "yellow":
                return YELLOW(context);
            case "amber":
                return AMBER(context);
            case "orange":
                return ORANGE(context);
            case "deep_orange":
                return DEEP_ORANGE(context);
            case "brown":
                return BROWN(context);
            case "gray":
                return GREY(context);
            case "blue_gray":
                return BLUE_GREY(context);
            case "black":
                return BLACK(context);
            default:
                return DEFAULT(context);
        }
    }
}
