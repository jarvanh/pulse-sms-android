package xyz.klinker.messenger.shared.util

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.MmsSettings
import xyz.klinker.messenger.shared.data.model.Message
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MediaSaver(val context: Context) {

    private var activity: Activity? = null

    constructor(activity: Activity) : this(activity as Context) {
        this.activity = activity
    }

    fun saveMedia(messageId: Long) {
        val message = DataSource.getMessage(context, messageId)
        saveMedia(message)
    }

    fun saveMedia(message: Message?) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            saveMessage(message)
        } else {
            if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity!!.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION)
            } else {
                saveMessage(message)
            }
        }
    }

    private fun saveMessage(message: Message?) {
        if (AndroidVersionUtil.isAndroidQ) {
            saveMessageQ(message)
            return
        }

        val directory = MmsSettings.saveDirectory
        val extension = MimeType.getExtension(message!!.mimeType!!)
        val date = (SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(message.timestamp) + ", " +
                TimeUtils.formatTime(context, Date(message.timestamp)))
                .replace(" ", "_").replace(",", "").replace(":", "_")
        val fileName = "media-${TimeUtils.now}-$date"

        val directoryFile = File(directory)
        if (!directoryFile.exists()) {
            directoryFile.mkdirs()
        }

        val dst = File(directory, fileName + extension)

        try {
            dst.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (MimeType.isStaticImage(message.mimeType)) {
            try {
                val bmp = ImageUtils.getBitmap(context, message.data)
                val stream = FileOutputStream(dst)
                bmp!!.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                stream.close()

                bmp.recycle()

                makeToast(R.string.saved)
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    makeToast(R.string.failed_to_save)
                } catch (x: Exception) {
                    // background thread
                }
            }
        } else {
            try {
                val `in` = context.contentResolver.openInputStream(Uri.parse(message.data))
                FileUtils.copy(`in`!!, dst)
                makeToast(R.string.saved)
            } catch (e: IOException) {
                e.printStackTrace()
                try {
                    makeToast(R.string.failed_to_save)
                } catch (x: Exception) {
                    // background thread
                }

            } catch (e: SecurityException) {
                e.printStackTrace()
                try {
                    makeToast(R.string.failed_to_save)
                } catch (x: Exception) {
                }
            }

        }

        updateMediaScanner(dst)
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun saveMessageQ(message: Message?) {
        try {
            val mime = message!!.mimeType!!
            val date = (SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(message.timestamp) + ", " +
                    TimeUtils.formatTime(context, Date(message.timestamp)))
                    .replace(" ", "_").replace(",", "").replace(":", "_")
            val fileName = "media-${TimeUtils.now}-$date"

            val relativeLocation = when {
                mime.contains("video") -> Environment.DIRECTORY_MOVIES
                mime.contains("audio") -> Environment.DIRECTORY_MUSIC
                else -> Environment.DIRECTORY_PICTURES
            } + "/Pulse"
            val contentUri = when {
                mime.contains("video") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                mime.contains("audio") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, message.mimeType!!)
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)

            val uri = context.contentResolver.insert(contentUri, contentValues)
            val outputStream = context.contentResolver.openOutputStream(uri!!)
            val `in` = context.contentResolver.openInputStream(Uri.parse(message.data))

            `in`!!.writeToOutputAndCleanup(outputStream as FileOutputStream)

            makeToast(R.string.saved)
        } catch (e: java.lang.Exception) {
            makeToast(R.string.failed_to_save)
        }
    }

    private fun updateMediaScanner(file: File) {
        try {
            MediaScannerConnection.scanFile(context,
                    arrayOf(file.toString()), null
            ) { path, uri ->
                Log.v("ExternalStorage", "Scanned $path:")
                Log.v("ExternalStorage", "-> uri=$uri")
            }
        } catch (e: java.lang.Exception) {

        }
    }

    private fun makeToast(@StringRes resource: Int) {
        if (context is Activity) {
            (context).runOnUiThread { Toast.makeText(context, resource, Toast.LENGTH_SHORT).show() }
        }
    }

    companion object {
        private const val REQUEST_STORAGE_PERMISSION = 119
    }
}
