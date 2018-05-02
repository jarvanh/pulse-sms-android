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

package xyz.klinker.messenger.shared.util

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.ContactsContract
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.annotation.DrawableRes
import android.support.media.ExifInterface
import android.support.v4.content.FileProvider
import android.util.Log

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Date

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.MmsSettings
import xyz.klinker.messenger.shared.data.model.Contact
import xyz.klinker.messenger.shared.data.model.Conversation

/**
 * Helper for working with images.
 */
object ImageUtils {

    /**
     * Gets a bitmap from the provided uri. Returns null if the bitmap cannot be found.
     */
    fun getBitmap(context: Context?, uri: String?) = try {
        MediaStore.Images.Media.getBitmap(context?.contentResolver, Uri.parse(uri))
    } catch (e: Exception) {
        null
    }

    /**
     * Clips a provided bitmap to a circle.
     */
    fun clipToCircle(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) {
            return null
        }

        val width = bitmap.width
        val height = bitmap.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val path = Path()
        path.addCircle(
                (width / 2).toFloat(),
                (height / 2).toFloat(),
                Math.min(width, height / 2).toFloat(),
                Path.Direction.CCW)

        val canvas = Canvas(outputBitmap)
        canvas.clipPath(path)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        bitmap.recycle()

        return outputBitmap
    }

    /**
     * Gets a bitmap from a contact URI.
     *
     * @param imageUri the contact uri to get an image for.
     * @param context  the application context.
     * @return the image bitmap.
     */
    fun getContactImage(imageUri: String?, context: Context?): Bitmap? {
        if (imageUri == null) {
            return null
        }

        return try {
            val stream = ContactsContract.Contacts.openContactPhotoInputStream(
                    context?.contentResolver, Uri.parse(imageUri), true)
            val bitmap = BitmapFactory.decodeStream(stream)
            stream?.closeSilent()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets the correct colors for a conversation based on its image.
     *
     * @param conversation the conversation to fill colors for.
     * @param context      the current context.
     */
    fun fillConversationColors(conversation: Conversation, context: Context) {
        if (conversation.phoneNumbers!!.contains(",")) {
            conversation.colors = ColorUtils.getRandomMaterialColor(context)
        } else {
            val contact = DataSource.getContact(context, conversation.phoneNumbers!!)
            if (contact != null) {
                conversation.colors = contact.colors
            } else {
                conversation.colors = ColorUtils.getRandomMaterialColor(context)
            }
        }
//        if (conversation.imageUri == null) {
//            conversation.colors = ColorUtils.getRandomMaterialColor(context)
//        } else {
//            val bitmap = ImageUtils.getContactImage(conversation.imageUri, context)
//            val colors = ImageUtils.extractColorSet(context, bitmap)
//
//            bitmap?.recycle()
//            conversation.colors = colors
//            conversation.imageUri = Uri
//                    .withAppendedPath(Uri.parse(conversation.imageUri),
//                            ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
//                    .toString()
//        }
    }

    /**
     * Gets the correct colors for a contact based on their image.
     *
     * @param contact      the conversation to fill colors for.
     * @param context      the current context.
     */
    fun fillContactColors(contact: Contact, imageUri: String?, context: Context) {
        contact.colors = ColorUtils.getRandomMaterialColor(context)
//        if (imageUri == null) {
//            contact.colors = ColorUtils.getRandomMaterialColor(context)
//        } else {
//            val bitmap = ImageUtils.getContactImage(imageUri, context)
//            val colors = ImageUtils.extractColorSet(context, bitmap)
//
//            bitmap?.recycle()
//            contact.colors = colors
//        }
    }

    /**
     * Extracts a material design color set from a provided bitmap.
     *
     * @param context the context.
     * @param bitmap  the bitmap.
     * @return the color set (defaults to random material color when it fails to extract with
     * palette).
     */
    fun extractColorSet(context: Context, bitmap: Bitmap?): ColorSet {
        return ColorUtils.getRandomMaterialColor(context)
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
    @Throws(IOException::class)
    fun scaleToSend(context: Context, uri: Uri, mimeType: String): Uri? {
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return uri

            var byteArr = ByteArray(0)
            val buffer = ByteArray(1024)
            var arraySize = 0
            var len = input.read(buffer)

            // convert the Uri to a byte array that we can manipulate
            while (len > -1) {
                if (len != 0) {
                    if (arraySize + len > byteArr.size) {
                        val newbuf = ByteArray((arraySize + len) * 2)
                        System.arraycopy(byteArr, 0, newbuf, 0, arraySize)
                        byteArr = newbuf
                    }

                    System.arraycopy(buffer, 0, byteArr, arraySize, len)
                    arraySize += len
                }

                len = input.read(buffer)
            }

            input.closeSilent()

            // with inJustDecodeBounds, we are telling the system just to get the resolution
            // of the image and not to decode anything else. This resolution
            // is used to calculate the in sample size
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(byteArr, 0, arraySize, options)
            val srcWidth = options.outWidth
            val srcHeight = options.outHeight

            val fileName = "image-" + Date().time + if (mimeType == MimeType.IMAGE_PNG) ".png" else ".jpg"

            val largerSide = if (srcHeight > srcWidth) srcHeight else srcWidth
            var scaleAmount = 1

            var scaled = generateBitmap(byteArr, arraySize, largerSide, scaleAmount)
            scaled = rotateBasedOnExifData(context, uri, scaled)
            var file = createFileFromBitmap(context, fileName, scaled, mimeType)
            val maxImageSize = MmsSettings.maxImageSize
            Log.v("ImageUtils", "file size: " + file.length() + ", mms size limit: " + maxImageSize)

            while (scaleAmount < 16 && file.length() > maxImageSize) {
                scaled.recycle()

                scaleAmount *= 2
                scaled = generateBitmap(byteArr, arraySize, largerSide, scaleAmount)
                scaled = rotateBasedOnExifData(context, uri, scaled)
                file = createFileFromBitmap(context, fileName, scaled, mimeType)

                Log.v("ImageUtils", "downsampling again. file size: " + file.length() + ", mms size limit: " + maxImageSize)
            }

            return ImageUtils.createContentUri(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } catch (e: OutOfMemoryError) {
            return null
        }

    }

    private fun generateBitmap(byteArr: ByteArray, arraySize: Int, largerSide: Int, scaleAmount: Int): Bitmap {
        val options = BitmapFactory.Options()

        // in sample size reduces the size of the image by this factor of 2
        options.inSampleSize = scaleAmount

        // these options set up the image coloring
        options.inPreferredConfig = Bitmap.Config.RGB_565 // could be Bitmap.Config.ARGB_8888 for higher quality
        options.inDither = true

        // these options set it up with the actual dimensions that you are looking for
        options.inDensity = largerSide
        options.inTargetDensity = largerSide * (1 / options.inSampleSize)

        // now we actually decode the image to these dimensions
        return BitmapFactory.decodeByteArray(byteArr, 0, arraySize, options)
    }

    private fun calculateInSampleSize(currentHeight: Int, currentWidth: Int, maxSize: Int): Int {
        var inSampleSize = 1
        val largerSide = if (currentHeight > currentWidth) currentHeight else currentWidth

        if (largerSide > maxSize) {
            // Calculate ratios of height and width to requested height and width

            Log.v("ImageUtils", "larger side: $largerSide, max size: $maxSize")

            inSampleSize = when {
                largerSide < maxSize * 2 -> 2
                largerSide < maxSize * 4 -> 4
                else -> 8
            }
        }

        return inSampleSize
    }

    private fun createFileFromBitmap(context: Context, name: String, bitmap: Bitmap, mimeType: String): File {
        var out: FileOutputStream? = null
        val file = File(context.filesDir, name)

        try {
            if (!file.exists()) {
                file.createNewFile()
            }

            out = FileOutputStream(file)
            bitmap.compress(if (mimeType == MimeType.IMAGE_PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                    75, out)
        } catch (e: IOException) {
            Log.e("Scale to Send", "failed to write output stream", e)
        } finally {
            try {
                out?.closeSilent()
            } catch (e: IOException) {
                Log.e("Scale to Send", "failed to close output stream", e)
            }

        }

        return file
    }

    private fun rotateBasedOnExifData(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        try {
            val `in` = context.contentResolver.openInputStream(uri)
            val exif = ExifInterface(`in`!!)

            val rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val matrix = Matrix()
            when (rotation) {
                ExifInterface.ORIENTATION_UNDEFINED, ExifInterface.ORIENTATION_NORMAL -> return bitmap
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    matrix.setRotate(180f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.setRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.setRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
                else -> return bitmap
            }

            val bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            return bmRotated
        } catch (e: Exception) {
            e.printStackTrace()
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        }

        return bitmap
    }

    /**
     * Creates a bitmap that is simply a single color.
     */
    fun createColoredBitmap(color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(color)
        return bitmap
    }

    /**
     * Overlays a drawable on top of a bitmap.
     */
    fun overlayBitmap(context: Context, bitmap: Bitmap, @DrawableRes overlay: Int) {
        val drawable = context.getDrawable(overlay)
        val size = context.resources.getDimensionPixelSize(R.dimen.overlay_size)
        val width = bitmap.width
        val height = bitmap.height

        val canvas = Canvas(bitmap)
        drawable!!.setBounds(width / 2 - size / 2, height / 2 - size / 2, width / 2 + size / 2, height / 2 + size / 2)
        drawable.draw(canvas)
    }

    /**
     * Create a content:// uri
     *
     * @param context application context
     * @param fileUri uri to the file (file://). If this is already a content uri, it will simply be returned.
     * @return sharable content:// uri to the file
     */
    fun createContentUri(context: Context, fileUri: Uri) = if (fileUri.toString().contains("content://")) {
        fileUri // we already have a content uri to pass to other applications
    } else {
        val file = File(fileUri.path)
        createContentUri(context, file)
    }

    /**
     * Create a content:// uri
     *
     * @param context application context
     * @param file Java file to be converted to content:// uri
     * @return sharable content:// uri to the file
     */
    fun createContentUri(context: Context?, file: File?) =
            if (context == null || file!!.path.contains("firebase -1")) {
                Uri.EMPTY
            } else {
                try {
                    FileProvider.getUriForFile(context,
                            context.packageName + ".provider", file)
                } catch (e: IllegalArgumentException) {
                    // photo doesn't exist
                    e.printStackTrace()
                    Uri.EMPTY
                }

            }

    fun getUriForPhotoCaptureIntent(context: Context) = try {
        val image = File(context.cacheDir, Date().time.toString() + ".jpg")
        if (!image.exists()) {
            image.parentFile.mkdirs()
            image.createNewFile()
        }

        createContentUri(context, image)
    } catch (e: Exception) {
        null
    }

    fun getUriForLatestPhoto(context: Context) = try {
        val image = lastFileModifiedPhoto(context.cacheDir)
        createContentUri(context, image)
    } catch (e: Exception) {
        null
    }

    private fun lastFileModifiedPhoto(fl: File): File? {
        val files = fl.listFiles { file -> file.isFile && file.name.contains(".jpg") }
        var lastMod = java.lang.Long.MIN_VALUE
        var choice: File? = null
        for (file in files) {
            if (file.lastModified() > lastMod) {
                choice = file
                lastMod = file.lastModified()
            }
        }

        return choice
    }

}
