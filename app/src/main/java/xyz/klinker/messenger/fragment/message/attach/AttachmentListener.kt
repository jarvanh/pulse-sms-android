package xyz.klinker.messenger.fragment.message.attach

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v13.view.inputmethod.InputConnectionCompat
import android.support.v13.view.inputmethod.InputContentInfoCompat
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
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
    private val selectedImageUris
        get() = attachManager.selectedImageUris

    private val attach: View by lazy { fragment.rootView!!.findViewById<View>(R.id.attach) }
    private val editImage: View? by lazy { fragment.rootView!!.findViewById<View>(R.id.edit_image) }
    private val selectedImageCount: View by lazy { fragment.rootView!!.findViewById<View>(R.id.selected_images) }
    private val selectedImageCountText: TextView by lazy { fragment.rootView!!.findViewById<View>(R.id.selected_images_text) as TextView }
    private val attachHolder: FrameLayout by lazy { fragment.rootView!!.findViewById<View>(R.id.attach_holder) as FrameLayout }
    private val messageEntry: EditText by lazy { fragment.rootView!!.findViewById<View>(R.id.message_entry) as EditText }

    var attachingFromCamera = false

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
                        attachManager.attachContact(contactFile)
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
        messageEntry.setText(text)
        messageEntry.setSelection(messageEntry.text.length)
    }

    override fun onImageSelected(uri: Uri, mimeType: String) {
        onImageSelected(uri, mimeType, false)
    }

    override fun onImageSelected(uri: Uri, mimeType: String, attachingFromCamera: Boolean) {
        if (selectedImageUris.size == 0 || attachingFromCamera) {
            // auto close the attach view after selecting the first image
            fragment.onBackPressed()
        }

        if (MimeType.isStaticImage(mimeType)) {
            if (!selectedImageUris.contains(uri.toString())) {
                attachManager.attachImage(uri)
                selectedImageUris.add(uri.toString())
            } else {
                selectedImageUris.remove(uri.toString())
                if (selectedImageUris.size > 0) {
                    attachManager.attachImage(Uri.parse(selectedImageUris[0]))
                }
            }

            when {
                selectedImageUris.size == 0 -> {
                    attachManager.clearAttachedData()
                    selectedImageUris.clear()
                    selectedImageCount.visibility = View.GONE
                    editImage?.visibility = View.VISIBLE
                }
                selectedImageUris.size > 1 -> {
                    selectedImageCount.visibility = View.VISIBLE
                    selectedImageCountText.text = selectedImageUris.size.toString()
                    editImage?.visibility = View.GONE
                }
                else -> {
                    selectedImageCount.visibility = View.GONE
                    editImage?.visibility = View.VISIBLE
                }
            }
        } else if (MimeType.isVideo(mimeType)) {
            videoEncoder.startVideoEncoding(uri)
            selectedImageUris.clear()
            selectedImageCount.visibility = View.GONE

            if (attachHolder.visibility == View.VISIBLE) {
                attach.performClick()
            }
        } else if (mimeType == MimeType.IMAGE_GIF) {
            attachManager.attachImage(uri)
            attachManager.attachedMimeType = MimeType.IMAGE_GIF
            editImage?.visibility = View.GONE
            selectedImageUris.clear()
            selectedImageCount.visibility = View.GONE

            if (attachHolder.visibility == View.VISIBLE) {
                attach.performClick()
            }
        }
    }

    override fun onRecorded(uri: Uri) {
        fragment.onBackPressed()
        attachManager.attachAudio(uri)
    }

    override fun onGalleryPicker() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        activity?.startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_GALLERY_PICKER_REQUEST)
    }

    override fun isCurrentlySelected(uri: Uri, mimeType: String) =
            selectedImageUris.contains(uri.toString()) ||
                    (attachManager.attachedUri != null && uri.toString() == attachManager.attachedUri.toString())

    override fun onCommitContent(inputContentInfo: InputContentInfoCompat?, flags: Int, bundle: Bundle?): Boolean {
        val mime = inputContentInfo?.description?.getMimeType(0)

        when {
            mime == MimeType.IMAGE_GIF -> {
                attachManager.attachImage(inputContentInfo.contentUri)
                attachManager.attachedMimeType = MimeType.IMAGE_GIF
                editImage?.visibility = View.GONE
            }
            mime != null && mime.contains("image/") -> {
                attachManager.attachImage(inputContentInfo.contentUri)
                attachManager.attachedMimeType = MimeType.IMAGE_PNG
            }
            mime != null && mime.contains(MimeType.VIDEO_MP4) -> {
                attachManager.attachImage(inputContentInfo.contentUri)
                attachManager.attachedMimeType = MimeType.VIDEO_MP4
                editImage?.visibility = View.GONE
            }
        }

        return true
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            attachManager.attachImage(UCrop.getOutput(data!!))

            attachManager.selectedImageUris.clear()
            selectedImageCount.visibility = View.GONE
        } else if (requestCode == RESULT_VIDEO_REQUEST) {
            fragment.onBackPressed()

            if (resultCode == RESULT_OK) {
                attachManager.attachImage(data!!.data)
                attachManager.attachedMimeType = MimeType.VIDEO_MP4
                editImage?.visibility = View.GONE

                attachManager.selectedImageUris.clear()
                selectedImageCount.visibility = View.GONE
            } else if (data != null) {
                val e = data.getSerializableExtra(MaterialCamera.ERROR_EXTRA) as Exception
                e.printStackTrace()
                Toast.makeText(activity, e.message, Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == Giphy.REQUEST_GIPHY) {
            fragment.onBackPressed()

            if (resultCode == RESULT_OK) {
                attachManager.attachImage(data!!.data)
                attachManager.attachedMimeType = MimeType.IMAGE_GIF
                editImage?.visibility = View.GONE

                attachManager.selectedImageUris.clear()
                selectedImageCount.visibility = View.GONE
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

            attachManager.attachImage(uri)
            if (mimeType != null && mimeType == MimeType.IMAGE_GIF) {
                attachManager.attachedMimeType = MimeType.IMAGE_GIF
                editImage?.visibility = View.GONE
            }

            attachManager.selectedImageUris.clear()
            selectedImageCount.visibility = View.GONE
        } else if (requestCode == RESULT_CAPTURE_IMAGE_REQUEST && resultCode == RESULT_OK && activity != null) {
            val uri = ImageUtils.getUriForLatestPhoto(activity!!)
            fragment.onBackPressed()
            attachManager.attachImage(uri)

            attachManager.selectedImageUris.clear()
            selectedImageCount.visibility = View.GONE
        }
    }

    companion object {
        val RESULT_VIDEO_REQUEST = 3
        val RESULT_GIPHY_REQUEST = 4
        val RESULT_GALLERY_PICKER_REQUEST = 6
        val RESULT_CAPTURE_IMAGE_REQUEST = 7
    }
}