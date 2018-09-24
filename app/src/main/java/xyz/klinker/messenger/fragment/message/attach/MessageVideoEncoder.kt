@file:Suppress("DEPRECATION")

package xyz.klinker.messenger.fragment.message.attach

import android.app.ProgressDialog
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import net.ypresto.androidtranscoder.MediaTranscoder
import net.ypresto.androidtranscoder.format.AndroidStandardFormatStrategy
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets
import xyz.klinker.messenger.R
import xyz.klinker.messenger.fragment.message.MessageListFragment
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.MmsSettings
import xyz.klinker.messenger.shared.util.ImageUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class MessageVideoEncoder(private val fragment: MessageListFragment) {

    private val activity: FragmentActivity? by lazy { fragment.activity }

    private val attachManager
        get() = fragment.attachManager

    private val editImage: View by lazy { fragment.rootView!!.findViewById<View>(R.id.edit_image) }

    fun startVideoEncoding(uri: Uri) {
        startVideoEncoding(uri, AndroidStandardFormatStrategy.Encoding.SD_LOW)
    }

    private fun startVideoEncoding(uri: Uri, encoding: AndroidStandardFormatStrategy.Encoding) {
        val original = File(uri.path)
        if (original.length() < MmsSettings.maxImageSize) {
            attachManager.attachImage(uri)
            attachManager.attachedMimeType = MimeType.VIDEO_MP4
            editImage.visibility = View.GONE
        } else {
            val file: File
            try {
                val outputDir = File(activity?.getExternalFilesDir(null), "outputs")
                outputDir.mkdir()

                file = File.createTempFile("transcode_video", ".mp4", outputDir)
            } catch (e: IOException) {
                Toast.makeText(activity, "Failed to create temporary file.", Toast.LENGTH_LONG).show()
                return
            }

            val resolver = activity?.contentResolver
            val parcelFileDescriptor: ParcelFileDescriptor?
            try {
                parcelFileDescriptor = resolver?.openFileDescriptor(uri, "r")
            } catch (e: FileNotFoundException) {
                Toast.makeText(activity, "File not found.", Toast.LENGTH_LONG).show()
                return
            }

            val progressDialog = ProgressDialog(activity)
            progressDialog.setCancelable(false)
            progressDialog.isIndeterminate = true
            progressDialog.setMessage(activity?.getString(R.string.preparing_video))

            if (parcelFileDescriptor == null) {
                return
            }

            val fileDescriptor = parcelFileDescriptor.fileDescriptor
            val listener = object : MediaTranscoder.Listener {
                override fun onTranscodeCanceled() {}
                override fun onTranscodeProgress(progress: Double) {}
                override fun onTranscodeFailed(exception: Exception) {
                    exception.printStackTrace()
                    Toast.makeText(activity,
                            "Failed to process video for sending: " + exception.message,
                            Toast.LENGTH_SHORT).show()

                    try {
                        progressDialog.dismiss()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }

                override fun onTranscodeCompleted() {
                    attachManager.attachImage(ImageUtils.createContentUri(activity, file))
                    attachManager.attachedMimeType = MimeType.VIDEO_MP4
                    editImage.visibility = View.GONE

                    try {
                        progressDialog.cancel()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            progressDialog.show()
            MediaTranscoder.getInstance().transcodeVideo(fileDescriptor, file.absolutePath,
                    MediaFormatStrategyPresets.createStandardFormatStrategy(encoding), listener)
        }
    }
}