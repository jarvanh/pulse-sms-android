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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.DrawableRes;
import android.support.media.ExifInterface;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.MmsSettings;
import xyz.klinker.messenger.shared.data.model.Contact;
import xyz.klinker.messenger.shared.data.model.Conversation;

/**
 * Helper for working with images.
 */
public class ImageUtils {

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
                (float) (width / 2),
                (float) (height / 2),
                (float) Math.min(width, (height / 2)),
                Path.Direction.CCW);

        final Canvas canvas = new Canvas(outputBitmap);
        canvas.clipPath(path);
        canvas.drawBitmap(bitmap, 0, 0, null);

        bitmap.recycle();

        return outputBitmap;
    }

    /**
     * Gets a bitmap from a contact URI.
     *
     * @param imageUri the contact uri to get an image for.
     * @param context  the application context.
     * @return the image bitmap.
     */
    public static Bitmap getContactImage(String imageUri, Context context) {
        if (imageUri == null) {
            return null;
        }

        try {
            InputStream stream = ContactsContract.Contacts.openContactPhotoInputStream(
                    context.getContentResolver(), Uri.parse(imageUri), true);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            stream.close();
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the correct colors for a conversation based on its image.
     *
     * @param conversation the conversation to fill colors for.
     * @param context      the current context.
     */
    public static void fillConversationColors(Conversation conversation, Context context) {
        if (conversation.getImageUri() == null) {
            conversation.setColors(ColorUtils.getRandomMaterialColor(context));
        } else {
            Bitmap bitmap = ImageUtils.getContactImage(conversation.getImageUri(), context);
            ColorSet colors = ImageUtils.extractColorSet(context, bitmap);

            if (bitmap != null) {
                bitmap.recycle();
            }

            if (colors != null) {
                conversation.setColors(colors);
                conversation.setImageUri(Uri
                        .withAppendedPath(Uri.parse(conversation.getImageUri()),
                                ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
                        .toString());
            } else {
                conversation.setColors(ColorUtils.getRandomMaterialColor(context));
                conversation.setImageUri(null);
            }
        }
    }

    /**
     * Gets the correct colors for a contact based on their image.
     *
     * @param contact      the conversation to fill colors for.
     * @param context      the current context.
     */
    public static void fillContactColors(Contact contact, String imageUri, Context context) {
        if (imageUri == null) {
            contact.setColors(ColorUtils.getRandomMaterialColor(context));
        } else {
            Bitmap bitmap = ImageUtils.getContactImage(imageUri, context);
            ColorSet colors = ImageUtils.extractColorSet(context, bitmap);

            if (bitmap != null) {
                bitmap.recycle();
            }

            if (colors != null) {
                contact.setColors(colors);
            } else {
                contact.setColors(ColorUtils.getRandomMaterialColor(context));
            }
        }
    }

    /**
     * Extracts a material design color set from a provided bitmap.
     *
     * @param context the context.
     * @param bitmap  the bitmap.
     * @return the color set (defaults to random material color when it fails to extract with
     * palette).
     */
    public static ColorSet extractColorSet(Context context, Bitmap bitmap) {
        return ColorUtils.getRandomMaterialColor(context);
//        try {
//            Palette p = Palette.from(bitmap).generate();
//
//            ColorSet colors = new ColorSet();
//            colors.color = p.getVibrantColor(Color.BLACK);
//            colors.colorDark = p.getDarkVibrantColor(Color.BLACK);
//            colors.colorLight = p.getLightVibrantColor(Color.BLACK);
//            colors.colorAccent = p.getMutedColor(Color.BLACK);
//
//            // if any of them get the default, then throw out the batch because it won't look
//            // good and a random material scheme will be better.
//            if (colors.color == Color.BLACK || colors.colorDark == Color.BLACK ||
//                    colors.colorLight == Color.BLACK || colors.colorAccent == Color.BLACK) {
//                return ColorUtils.getRandomMaterialColor(context);
//            } else {
//                return colors;
//            }
//        } catch (Exception e) {
//            return null;
//        }
    }

    /**
     * Scales a bitmap file to a lower resolution so that it can be sent over MMS. Most carriers
     * have a 1 MB limit, so we'll scale to under that. This method will create a new file in the
     * application memory.
     */
    public static Uri scaleToSend(Context context, Uri uri, String mimeType) throws IOException {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);

            byte[] byteArr = new byte[0];
            byte[] buffer = new byte[1024];
            int arraySize = 0;
            int len;

            // convert the Uri to a byte array that we can manipulate
            while ((len = input.read(buffer)) > -1) {
                if (len != 0) {
                    if (arraySize + len > byteArr.length) {
                        byte[] newbuf = new byte[(arraySize + len) * 2];
                        System.arraycopy(byteArr, 0, newbuf, 0, arraySize);
                        byteArr = newbuf;
                    }

                    System.arraycopy(buffer, 0, byteArr, arraySize, len);
                    arraySize += len;
                }
            }

            try {
                input.close();
            } catch(Exception e) { }

            // with inJustDecodeBounds, we are telling the system just to get the resolution
            // of the image and not to decode anything else. This resolution
            // is used to calculate the in sample size
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(byteArr, 0, arraySize, options);
            int srcWidth = options.outWidth;
            int srcHeight = options.outHeight;

            String fileName = "image-" + new Date().getTime();

            if (mimeType.equals(MimeType.INSTANCE.getIMAGE_PNG())) {
                fileName += ".png";
            } else {
                fileName += ".jpg";
            }

            int largerSide = srcHeight > srcWidth ? srcHeight : srcWidth;
            int scaleAmount = 1;

            Bitmap scaled = generateBitmap(byteArr, arraySize, largerSide, scaleAmount);
            scaled = rotateBasedOnExifData(context, uri, scaled);
            File file = createFileFromBitmap(context, fileName, scaled, mimeType);
            Log.v("ImageUtils", "file size: " + file.length() + ", mms size limit: " + MmsSettings.get(context).maxImageSize);

            while (scaleAmount < 16 && file.length() > MmsSettings.get(context).maxImageSize) {
                scaled.recycle();

                scaleAmount = scaleAmount * 2;
                scaled = generateBitmap(byteArr, arraySize, largerSide, scaleAmount);
                scaled = rotateBasedOnExifData(context, uri, scaled);
                file = createFileFromBitmap(context, fileName, scaled, mimeType);

                Log.v("ImageUtils", "downsampling again. file size: " + file.length() + ", mms size limit: " + MmsSettings.get(context).maxImageSize);
            }

            return ImageUtils.createContentUri(context, file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap generateBitmap(byte[] byteArr, int arraySize, int largerSide, int scaleAmount) {
        final BitmapFactory.Options options = new BitmapFactory.Options();

        // in sample size reduces the size of the image by this factor of 2
        options.inSampleSize = scaleAmount;

        // these options set up the image coloring
        options.inPreferredConfig = Bitmap.Config.RGB_565; // could be Bitmap.Config.ARGB_8888 for higher quality
        options.inDither = true;

        // these options set it up with the actual dimensions that you are looking for
        options.inDensity = largerSide;
        options.inTargetDensity = largerSide * (1 / options.inSampleSize);

        // now we actually decode the image to these dimensions
        return BitmapFactory.decodeByteArray(byteArr, 0, arraySize, options);
    }

    private static int calculateInSampleSize(int currentHeight, int currentWidth, int maxSize) {
        int inSampleSize = 1;
        int largerSide = currentHeight > currentWidth ? currentHeight : currentWidth;

        if (largerSide > maxSize) {
            // Calculate ratios of height and width to requested height and width

            Log.v("ImageUtils", "larger side: " + largerSide + ", max size: " + maxSize);

            if (largerSide < maxSize * 2) {
                inSampleSize = 2;
            } else if (largerSide < maxSize * 4) {
                inSampleSize = 4;
            } else {
                inSampleSize = 8;
            }
        }

        return inSampleSize;
    }

    private static File createFileFromBitmap(Context context, String name, Bitmap bitmap, String mimeType) {
        FileOutputStream out = null;
        File file = new File(context.getFilesDir(), name);

        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            out = new FileOutputStream(file);
            bitmap.compress(mimeType.equals(MimeType.INSTANCE.getIMAGE_PNG()) ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG,
                    75, out);
        } catch (IOException e) {
            Log.e("Scale to Send", "failed to write output stream", e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Log.e("Scale to Send", "failed to close output stream", e);
            }
        }

        return file;
    }

    public static Bitmap rotateBasedOnExifData(Context context, Uri uri, Bitmap bitmap) {
        try {
            InputStream in = context.getContentResolver().openInputStream(uri);
            ExifInterface exif = new ExifInterface(in);

            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            Matrix matrix = new Matrix();
            switch (rotation) {
                case ExifInterface.ORIENTATION_UNDEFINED:
                case ExifInterface.ORIENTATION_NORMAL:
                    return bitmap;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.setScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setRotate(180);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.setRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(270);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(270);
                    break;
                default:
                    return bitmap;
            }

            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * Creates a bitmap that is simply a single color.
     */
    public static Bitmap createColoredBitmap(int color) {
        Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(color);
        return bitmap;
    }

    /**
     * Overlays a drawable on top of a bitmap.
     */
    public static void overlayBitmap(Context context, Bitmap bitmap, @DrawableRes int overlay) {
        Drawable drawable = context.getDrawable(overlay);
        int size = context.getResources().getDimensionPixelSize(R.dimen.overlay_size);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(width / 2 - size / 2, height / 2 - size / 2, width / 2 + size / 2, height / 2 + size / 2);
        drawable.draw(canvas);
    }

    /**
     * Create a content:// uri
     *
     * @param context application context
     * @param fileUri uri to the file (file://). If this is already a content uri, it will simply be returned.
     * @return sharable content:// uri to the file
     */
    public static Uri createContentUri(Context context, Uri fileUri) {
        if (fileUri.toString().contains("content://")) {
            return fileUri; // we already have a content uri to pass to other applications
        } else {
            File file = new File(fileUri.getPath());
            return createContentUri(context, file);
        }
    }

    /**
     * Create a content:// uri
     *
     * @param context application context
     * @param file Java file to be converted to content:// uri
     * @return sharable content:// uri to the file
     */
    public static Uri createContentUri(Context context, File file) {
        if (context == null || file.getPath().contains("firebase -1")) {
            return Uri.EMPTY;
        } else {
            try {
                return FileProvider.getUriForFile(context,
                        context.getPackageName() + ".provider", file);
            } catch (IllegalArgumentException e) {
                // photo doesn't exist
                e.printStackTrace();
                return Uri.EMPTY;
            }
        }
    }

    public static Uri getUriForPhotoCaptureIntent(Context context) {
        try {
            File image = new File(context.getCacheDir(), new Date().getTime() + ".jpg");
            if (!image.exists()) {
                image.getParentFile().mkdirs();
                image.createNewFile();
            }

            return createContentUri(context, image);
        } catch (Exception e) {
            return null;
        }

    }

    public static Uri getUriForLatestPhoto(Context context) {
        try {
            File image = lastFileModifiedPhoto(context.getCacheDir());
            return createContentUri(context, image);
        } catch (Exception e) {
            return null;
        }
    }

    private static File lastFileModifiedPhoto(File fl) {
        File[] files = fl.listFiles(file -> file.isFile() && file.getName().contains(".jpg"));
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        return choice;
    }

}
