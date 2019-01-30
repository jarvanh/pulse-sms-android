package xyz.klinker.messenger.fragment.message.send

import xyz.klinker.messenger.fragment.message.MessageListFragment

class PermissionHelper(private val fragment: MessageListFragment) {

    private val attachManager
        get() = fragment.attachInitializer

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
        try {
            when (requestCode) {
                PERMISSION_STORAGE_REQUEST -> attachManager.attachImage(true)
                PERMISSION_AUDIO_REQUEST -> attachManager.recordAudio(true)
                PERMISSION_LOCATION_REQUEST -> attachManager.attachLocation(true)
                PERMISSION_CAMERA_REQUEST -> attachManager.captureImage(true)
                else -> return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return true
    }

    companion object {
        const val PERMISSION_STORAGE_REQUEST = 1
        const val PERMISSION_AUDIO_REQUEST = 2
        const val PERMISSION_CAMERA_REQUEST = 3
        const val PERMISSION_LOCATION_REQUEST = 5
    }

}