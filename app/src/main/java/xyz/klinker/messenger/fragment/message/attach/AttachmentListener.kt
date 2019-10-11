package xyz.klinker.messenger.fragment.message.attach

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.fragment.app.FragmentActivity
import com.afollestad.materialcamera.MaterialCamera
import com.yalantis.ucrop.UCrop
import xyz.klinker.giphy.Giphy
import xyz.klinker.messenger.R
import xyz.klinker.messenger.fragment.message.MessageListFragment
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.util.ImageUtils
import xyz.klinker.messenger.shared.util.vcard.VcardWriter
import xyz.klinker.messenger.shared.util.listener.AttachContactListener
import xyz.klinker.messenger.shared.util.listener.AudioRecordedListener
import xyz.klinker.messenger.shared.util.listener.ImageSelectedListener
import xyz.klinker.messenger.shared.util.listener.TextSelectedListener

class AttachmentListener(private val fragment: MessageListFragment)
    : ImageSelectedListener, AudioRecordedListener, AttachContactListener, TextSelectedListener,
        InputConnectionCompat.OnCommitContentListener {

    private val videoEncoder = MessageVideoEncoder(fragment)
    private val activity: FragmentActivity? by lazy { fragment.activity }

    private val attachManager
        get() = fragment.attachManager
    private val currentlyAttached
        get() = attachManager.currentlyAttached

    private val attach: View by lazy { fragment.rootView!!.findViewById<View>(R.id.attach) }
    private val attachHolder: FrameLayout by lazy { fragment.rootView!!.findViewById<View>(R.id.attach_holder) as FrameLayout }
    private val messageEntry: EditText by lazy { fragment.rootView!!.findViewById<View>(R.id.message_entry) as EditText }

    @SuppressLint("SetTextI18n")
    override fun onContactAttached(firstName: String, lastName: String, phone: String) {
        if (activity == null) {
            return
        }

        fragment.onBackPressed()

        AlertDialog.Builder(activity!!)
            .setItems(R.array.attach_contact_options) { _, i ->
                when (i) {
                    0 -> try {
                        val contactFile = VcardWriter.writeContactCard(activity!!, firstName, lastName, phone)
                        attachManager.attachMedia(contactFile, MimeType.TEXT_VCARD)
                    } catch (e: Exception) {
                    }

                    1 -> {
                        if (messageEntry.text.isEmpty()) {
                            messageEntry.setText("$firstName $lastName: $phone")
                        } else {
                            messageEntry.setText(messageEntry.text.toString() + "\n$firstName $lastName: $phone")
                        }
                    }
                }
            }.show()
    }

    override fun onTextSelected(text: String) {
        if (text.contains("maps")) {
            // append the map link to the text
            messageEntry.setText(messageEntry.text.toString().trim() + " " + text)
        } else {
            messageEntry.setText(text)
        }

        messageEntry.setSelection(messageEntry.text.length)
    }

    override fun onImageSelected(uri: Uri, mimeType: String) {
        onImageSelected(uri, mimeType, false)
    }

    override fun onImageSelected(uri: Uri, mimeType: String, attachingFromCamera: Boolean) {
        if (currentlyAttached.isEmpty() || attachingFromCamera) {
            // auto close the attach view after selecting the first image
            fragment.onBackPressed()
        }

        if (MimeType.isVideo(mimeType)) {
            videoEncoder.startVideoEncoding(uri)
            if (attachHolder.visibility == View.VISIBLE) {
                attach.performClick()
            }
        } else {
            if (!isCurrentlySelected(uri)) {
                attachManager.attachMedia(uri, mimeType)
            } else {
                attachManager.removeAttachment(uri)
            }
        }
    }

    override fun onRecorded(uri: Uri) {
        fragment.onBackPressed()
        attachManager.attachMedia(uri, MimeType.AUDIO_MP4)
    }

    override fun onGalleryPicker() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        activity?.startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_GALLERY_PICKER_REQUEST)
    }

    override fun isCurrentlySelected(uri: Uri) = currentlyAttached.firstOrNull { it.mediaUri.toString() == uri.toString() } != null

    override fun onCommitContent(inputContentInfo: InputContentInfoCompat?, flags: Int, bundle: Bundle?): Boolean {
        val mime = inputContentInfo?.description?.getMimeType(0)
        if (mime != null) {
            attachManager.attachMedia(inputContentInfo.contentUri, mime)
        }

        return true
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            attachManager.editingImage?.setup(attachManager, UCrop.getOutput(data!!)!!, MimeType.IMAGE_JPEG)
        } else if (requestCode == RESULT_VIDEO_REQUEST) {
            fragment.onBackPressed()

            if (resultCode == RESULT_OK) {
                attachManager.attachMedia(data!!.data, MimeType.VIDEO_MP4)
            } else if (data != null) {
                val e = data.getSerializableExtra(MaterialCamera.ERROR_EXTRA) as Exception
                e.printStackTrace()
                Toast.makeText(activity, e.message, Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == Giphy.REQUEST_GIPHY) {
            fragment.onBackPressed()

            if (resultCode == RESULT_OK) {
                attachManager.attachMedia(data!!.data, MimeType.IMAGE_GIF)
            }
        } else if (requestCode == RESULT_GALLERY_PICKER_REQUEST && resultCode == RESULT_OK
                && data != null && data.data != null) {
            fragment.onBackPressed()

            val uri = data.data
            val uriString = data.dataString
            var mimeType: String? = MimeType.IMAGE_JPEG
            if (uriString!!.contains("content://")) {
                mimeType = activity?.contentResolver?.getType(uri!!)
            }

            if (mimeType != null) {
                if (MimeType.isVideo(mimeType)) {
                    videoEncoder.startVideoEncoding(uri!!)
                } else {
                    attachManager.attachMedia(uri, mimeType)
                }
            }
        } else if (requestCode == RESULT_GALLERY_PICKER_REQUEST && resultCode == RESULT_OK
                && data != null && data.clipData != null) {
            fragment.onBackPressed()

            val clipData = data.clipData!!
            for (i in 0..clipData.itemCount) {
                val clip = clipData.getItemAt(i)
                val uri = clip.uri
                val uriString = uri.toString()
                var mimeType: String? = MimeType.IMAGE_JPEG
                if (uriString.contains("content://")) {
                    mimeType = activity?.contentResolver?.getType(uri)
                }

                onImageSelected(uri, mimeType!!)
            }
        } else if (requestCode == RESULT_CAPTURE_IMAGE_REQUEST && resultCode == RESULT_OK && activity != null) {
            val uri = ImageUtils.getUriForLatestPhoto(activity!!)
            fragment.onBackPressed()
            attachManager.attachMedia(uri, MimeType.IMAGE_JPEG)
        }
    }

    companion object {
        val RESULT_VIDEO_REQUEST = 3
        val RESULT_GIPHY_REQUEST = 4
        val RESULT_GALLERY_PICKER_REQUEST = 6
        val RESULT_CAPTURE_IMAGE_REQUEST = 7
    }
}