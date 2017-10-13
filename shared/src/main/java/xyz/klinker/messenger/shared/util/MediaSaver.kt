package xyz.klinker.messenger.shared.util

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.widget.Toast

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.MmsSettings
import xyz.klinker.messenger.shared.data.model.Message

class MediaSaver {

    private var activity: Activity? = null
    private var context: Context? = null

    constructor(activity: Activity) {
        this.activity = activity
        this.context = activity
    }

    constructor(context: Context) {
        this.context = context
    }

    fun saveMedia(messageId: Long) {
        val message = DataSource.getMessage(context!!, messageId)
        saveMedia(message)
    }

    fun saveMedia(message: Message?) {
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
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
        val directory = MmsSettings.saveDirectory
        val extension = MimeType.getExtension(message!!.mimeType!!)
        val fileName = "media-" + message.timestamp
        var dst = File(directory, fileName + extension)

        var count = 1
        while (dst.exists()) {
            dst = File(directory, fileName + "-" + Integer.toString(count) + extension)
            count++
        }

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

                val values = ContentValues(3)

                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                values.put(MediaStore.Images.Media.MIME_TYPE, message.mimeType)
                values.put(MediaStore.MediaColumns.DATA, dst.path)

                context!!.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

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
                val `in` = context!!.contentResolver.openInputStream(Uri.parse(message.data))
                FileUtils.copy(`in`, dst)
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

    private fun updateMediaScanner(file: File) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val contentUri = Uri.fromFile(file)
        mediaScanIntent.data = contentUri
        context!!.sendBroadcast(mediaScanIntent)
    }

    private fun makeToast(@StringRes resource: Int) {
        if (context is Activity) {
            (context as Activity).runOnUiThread { Toast.makeText(context, resource, Toast.LENGTH_SHORT).show() }
        }
    }

    companion object {
        private val REQUEST_STORAGE_PERMISSION = 119
    }
}
