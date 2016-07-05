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

import android.content.Context;

import xyz.klinker.messenger.R;

/**
 * Helper class for working with colors.
 */
public class ColorUtil {

    public int getRandomMaterialColor(Context context) {
        int num = (int) (Math.random() * (18 + 1));

        switch (num) {
            case 0:     return context.getResources().getColor(R.color.materialRed);
            case 1:     return context.getResources().getColor(R.color.materialPink);
            case 2:     return context.getResources().getColor(R.color.materialPurple);
            case 3:     return context.getResources().getColor(R.color.materialDeepPurple);
            case 4:     return context.getResources().getColor(R.color.materialIndigo);
            case 5:     return context.getResources().getColor(R.color.materialBlue);
            case 6:     return context.getResources().getColor(R.color.materialLightBlue);
            case 7:     return context.getResources().getColor(R.color.materialCyan);
            case 8:     return context.getResources().getColor(R.color.materialTeal);
            case 9:     return context.getResources().getColor(R.color.materialGreen);
            case 10:    return context.getResources().getColor(R.color.materialLightGreen);
            case 11:    return context.getResources().getColor(R.color.materialLime);
            case 12:    return context.getResources().getColor(R.color.materialYellow);
            case 13:    return context.getResources().getColor(R.color.materialAmber);
            case 14:    return context.getResources().getColor(R.color.materialOrange);
            case 15:    return context.getResources().getColor(R.color.materialDeepOrange);
            case 16:    return context.getResources().getColor(R.color.materialBrown);
            case 17:    return context.getResources().getColor(R.color.materialGrey);
            case 18:    return context.getResources().getColor(R.color.materialBlueGrey);
            default:    throw new RuntimeException("Invalid random color: " + num);
        }
    }
}
