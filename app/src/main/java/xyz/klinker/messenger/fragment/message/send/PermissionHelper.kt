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
                else -> return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return true
    }

    companion object {
        val PERMISSION_STORAGE_REQUEST = 1
        val PERMISSION_AUDIO_REQUEST = 2
        val PERMISSION_LOCATION_REQUEST = 5

    }

}