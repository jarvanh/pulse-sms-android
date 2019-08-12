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
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.annotation.StringRes

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.MmsSettings
import xyz.klinker.messenger.shared.data.model.Message
import java.util.*

class MediaSaver {

    private var activity: Activity? = null
    private var context: Context? = null

    constructor(activity: Activity?) {
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
        val date = (SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(message.timestamp) + ", " +
                TimeUtils.formatTime(context!!, Date(message.timestamp)))
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

                val values = ContentValues(3)

                values.put(MediaStore.Images.Media.DATE_TAKEN, TimeUtils.now)
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
