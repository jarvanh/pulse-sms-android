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
import android.graphics.Canvas;
import android.graphics.Path;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v7.graphics.Palette;

import java.io.InputStream;

import xyz.klinker.messenger.data.ColorSet;
import xyz.klinker.messenger.data.model.Conversation;

/**
 * Helper for working with images.
 */
public class ImageUtil {

    /**
     * Gets a bitmap from the provided uri. Returns null if the bitmap cannot be found.
     */
    public static Bitmap getBitmap(Context context, String uri) {
        try {
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.parse(uri));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Clips a provided bitmap to a circle.
     */
    public static Bitmap clipToCircle(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final Bitmap outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        final Path path = new Path();
        path.addCircle(
                (float)(width / 2),
                (float)(height / 2),
                (float) Math.min(width, (height / 2)),
                Path.Direction.CCW);

        final Canvas canvas = new Canvas(outputBitmap);
        canvas.clipPath(path);
        canvas.drawBitmap(bitmap, 0, 0, null);
        return outputBitmap;
    }

    /**
     * Gets a bitmap from a contact URI.
     *
     * @param imageUri the contact uri to get an image for.
     * @param context the application context.
     * @return the image bitmap.
     */
    public static Bitmap getContactImage(String imageUri, Context context) {
        InputStream stream = ContactsContract.Contacts.openContactPhotoInputStream(
                context.getContentResolver(), Uri.parse(imageUri), true);
        return BitmapFactory.decodeStream(stream);
    }

    /**
     * Gets the correct colors for a conversation based on its image.
     *
     * @param conversation the conversation to fill colors for.
     * @param context the current context.
     */
    public static void fillConversationColors(Conversation conversation, Context context) {
        if (conversation.imageUri == null) {
            conversation.colors = ColorUtil.getRandomMaterialColor(context);
        } else {
            Bitmap bitmap = ImageUtil.getContactImage(conversation.imageUri, context);
            ColorSet colors = ImageUtil.extractColorSet(context, bitmap);

            if (colors != null) {
                conversation.colors = colors;
                conversation.imageUri = Uri
                        .withAppendedPath(Uri.parse(conversation.imageUri),
                                ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
                        .toString();
            } else {
                conversation.colors = ColorUtil.getRandomMaterialColor(context);
                conversation.imageUri = null;
            }
        }
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
            return null;
        }
    }

}
