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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.graphics.Palette;

import java.io.InputStream;

import xyz.klinker.messenger.data.ColorSet;

/**
 * Helper for working with images.
 */
public class ImageUtil {

    /**
     * Extracts a color set from a contact photo uri.
     *
     * @param context the current application context.
     * @param uri the image uri.
     * @return the color set.
     */
    public static ColorSet extractContactColorSet(Context context, String uri) {
        return extractColorSet(context, ContactsContract.Contacts.openContactPhotoInputStream(
                context.getContentResolver(), Uri.parse(uri), true));
    }

    private static ColorSet extractColorSet(Context context, InputStream inputStream) {
        return extractColorSet(context, BitmapFactory.decodeStream(inputStream));
    }

    /**
     * Extracts a material design color set from a provided bitmap.
     *
     * @param context the context.
     * @param bitmap the bitmap.
     * @return the color set (defaults to random material color when it fails to extract with
     *         palette).
     */
    public static ColorSet extractColorSet(Context context, Bitmap bitmap) {
        ColorSet defaults = ColorUtil.getRandomMaterialColor(context);

        try {
            Palette p = Palette.from(bitmap).generate();

            ColorSet colors = new ColorSet();
            colors.color = p.getVibrantColor(defaults.color);
            colors.colorDark = p.getDarkVibrantColor(defaults.colorDark);
            colors.colorLight = p.getLightVibrantColor(defaults.colorLight);
            colors.colorAccent = p.getMutedColor(defaults.colorAccent);

            return colors;
        } catch (Exception e) {
            return defaults;
        }
    }

}
