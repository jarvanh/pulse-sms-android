package xyz.klinker.messenger.api.implementation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.support.media.ExifInterface;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class ImageScaler {
    /**
     * Scales a bitmap file to a lower resolution so that it can be sent over MMS. Most carriers
     * have a 1 MB limit, so we'll scale to under that. This method will create a new file in the
     * application memory.
     */
    public static Bitmap scaleToSend(Context context, Uri uri) throws IOException {
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

            // start generating bitmaps and checking the size against the max size
            Bitmap scaled = generateBitmap(byteArr, arraySize, srcWidth, srcHeight);
            return rotateBasedOnExifData(context, uri, scaled);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap generateBitmap(byte[] byteArr, int arraySize, int srcWidth, int srcHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();

        // in sample size reduces the size of the image by this factor of 2
        options.inSampleSize = 2;

        // these options set up the image coloring
        options.inPreferredConfig = Bitmap.Config.RGB_565; // could be Bitmap.Config.ARGB_8888 for higher quality
        options.inDither = true;

        int largerSide = srcHeight > srcWidth ? srcHeight : srcWidth;

        // these options set it up with the actual dimensions that you are looking for
        options.inDensity = largerSide;
        options.inTargetDensity = largerSide * (1 / options.inSampleSize);

        // now we actually decode the image to these dimensions
        return BitmapFactory.decodeByteArray(byteArr, 0, arraySize, options);
    }

    private static int calculateInSampleSize(int currentHeight, int currentWidth, int maxSize) {
        int inSampleSize = 1;

        if (currentHeight > maxSize || currentWidth > maxSize) {
            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) currentHeight / (float) maxSize);
            final int widthRatio = Math.round((float) currentWidth / (float) maxSize);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
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
}
